package cn.qingweico.io;


import cn.hutool.core.io.FileUtil;
import cn.qingweico.concurrent.pool.ThreadPoolBuilder;
import cn.qingweico.constants.Constants;
import cn.qingweico.constants.FileSuffixConstants;
import cn.qingweico.constants.Symbol;
import cn.qingweico.convert.ByteUnitConverter;
import cn.qingweico.convert.TimeUnitConverter;
import cn.qingweico.model.*;
import cn.qingweico.network.NetworkUtils;
import com.google.common.io.ByteStreams;
import com.google.common.io.MoreFiles;
import com.google.common.io.Resources;
import io.rsocket.metadata.WellKnownMimeType;
import jodd.util.StringPool;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOExceptionList;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.tika.Tika;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 通用的文件处理函数
 * 文件处理的性能考虑
 * - 大文件处理,使用缓冲行处理
 * - 超大文件,使用 {@link FileChannel} 和内存映射
 *
 * @author zqw
 * @date 2021/10/31
 */
@Slf4j
public final class FileUtils {
    private FileUtils() {
    }

    static ExecutorService pool = ThreadPoolBuilder.builder().build();

    /**
     * {@link ArchiveStreamFactory#detect(InputStream)}
     * deps on {@link ArchiveStreamFactory#getInputStreamArchiveNames}, rather than {@link ArchiveStreamFactory#getOutputStreamArchiveNames()}
     */
    static Set<String> archiveFileTypes = ArchiveStreamFactory.DEFAULT.getInputStreamArchiveNames();

    static Set<String> compressFileTypes = new CompressorStreamFactory().getInputStreamCompressorNames();

    static Tika tika = new Tika();
    // add more...
    static List<FileMagic> ignoredFileMagics = List.of(FileMagic.PDF, FileMagic.OOXML);

    /**
     * Read file and put in the ArrayList
     *
     * @param filename {@code String} filename
     * @param list     {@code ArrayList<String>>}
     */
    public static void readFileToArrayList(String filename, ArrayList<String> list) {

        if (filename == null) {
            log.error("file is null");
            return;
        }
        if (list == null) {
            log.error("list is null");
            return;
        }
        Scanner scanner;
        try {
            File file = new File(filename);
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                scanner = new Scanner(new BufferedInputStream(fis), StandardCharsets.UTF_8);
                scanner.useLocale(Locale.ENGLISH);
            } else {
                log.error("file {} is not exist", filename);
                return;
            }

        } catch (IOException ioe) {
            log.error("Cannot open {}", filename);
            return;
        }

        if (scanner.hasNextLine()) {
            String contents = scanner.useDelimiter("\\A").next();
            int start = firstCharacterIndex(contents, 0);
            for (int i = start + 1; i <= contents.length(); ) {
                if (i == contents.length() || !Character.isLetter(contents.charAt(i))) {
                    String word = contents.substring(start, i).toLowerCase();
                    list.add(word);
                    start = firstCharacterIndex(contents, i);
                    i = start + 1;
                } else {
                    i++;
                }
            }
        }
    }

    /**
     * 返回字符串中第一个字母的索引
     *
     * @param s     字符串
     * @param start 起始位置
     * @return "13llo, 0" => 2
     */
    private static int firstCharacterIndex(String s, int start) {

        for (int i = start; i < s.length(); i++) {
            if (Character.isLetter(s.charAt(i))) {
                return i;
            }
        }
        return s.length();
    }


    public static void copyFileByStream(File source, File target) {
        try (InputStream is = new FileInputStream(source); OutputStream os = new FileOutputStream(target)) {
            byte[] buffer = new byte[Constants.KB];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void copyFileByChannel(File source, File target) {
        try (FileInputStream fis = new FileInputStream(source); FileOutputStream fos = new FileOutputStream(target)) {
            FileChannel sourceChannel = fis.getChannel();
            FileChannel targetChannel = fos.getChannel();
            for (long count = sourceChannel.size(); count > 0; ) {
                long transferred = sourceChannel.transferTo(sourceChannel.position(), count, targetChannel);
                sourceChannel.position(sourceChannel.position() + transferred);
                count -= transferred;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * NIO 和操作系统底层密切相关, 每个平台都有自己实现的文件系统逻辑
     * <p>
     * {@link FileSystemProvider}
     * {@code Windows} {@see sun.nio.fs.WindowsFileSystemProvider}
     * {@code Linux Mac} {@see UnixFileSystemProvider -> UnixCopyFileSystem#transfer() ->  UnixCopyFile.c}
     *
     * @param source {@link  Path}
     * @param target {@link Path}
     */
    public static void fileCopy(Path source, Path target) {
        try {
            Files.copy(source, target);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 使用 Files 工具类快速读取文件
     *
     * @param filename [路径]文件名称
     * @return 文本字符流
     * 快速写入文件{@link Files#writeString(Path, CharSequence, OpenOption...)}
     */
    public static String fastReadFile(String filename, boolean isClassPath) {
        String result;
        // 读取ClassPath路径
        if (isClassPath) {
            URL url = FileUtils.class.getClassLoader().getResource(filename);
            if (url == null) {
                log.error("[classpath] : 文件 {} 不存在", filename);
                return Symbol.EMPTY;
            }
            try {
                byte[] bytes = Files.readAllBytes(Paths.get(url.toURI()));
                // 无论使用readString or readAllBytes 读取字节码文件时都会乱码, 文本文件则正常
                result = new String(bytes, 0, bytes.length);
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }

        } else {
            // 读取项目根目录路径
            try {
                result = Files.readString(Paths.get(filename));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    /**
     * 快速遍历文件夹并打印出其中所有的文件
     *
     * @param directory 文件夹名称
     * @see #fileList(File) 使用递归
     */
    public static void fileList(String directory) {
        try (Stream<Path> pathStream = Files.walk(Paths.get(directory))) {
            pathStream.filter(Files::isRegularFile).forEach(System.out::println);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Recursively traverse all the files in a folder
     *
     * @param target file
     * @see FileUtils#fileList(String)
     */
    public static void fileList(File target) {
        if (target.isFile()) {
            return;
        }
        File[] files = target.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            System.out.println(file.getName());
            fileList(file);
        }
    }


    /**
     * 以树状结构打印文件夹内容
     *
     * @param directory 文件夹路径
     */
    public static void printFileTree(String directory) {
        Path rootPath = Paths.get(directory);
        if (!Files.isDirectory(rootPath)) {
            Print.err("路径不是目录", directory);
            return;
        }
        System.out.println(rootPath.toAbsolutePath());
        printTree(rootPath, 0);
    }

    private static void printTree(Path path, int depth) {
        String indent = "    ".repeat(depth);
        String prefix = depth == 0 ? "" : "├── ";
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                System.out.println(indent + prefix + entry.getFileName());
                if (Files.isDirectory(entry)) {
                    // 递归子目录
                    printTree(entry, depth + 1);
                }
            }
        } catch (IOException e) {
            System.err.println("无法访问: " + path + " (" + e.getMessage() + ")");
        }
    }

    /**
     * 按行读取文本文件,并以 {@code delimiter} 连接成一行String
     *
     * @param file      [路径]文件名称
     * @param delimiter 分割符
     * @return the collected string of a line
     */
    public static String readAsAline(String file, String delimiter) {
        String collected;
        try (Stream<String> lines = Files.lines(Paths.get(file))) {
            // joining : 在每个元素之间使用的分隔符
            // eg: 1\n2\n3 + "-" => 1-2-3; not use prefix and suffix
            collected = lines.collect(Collectors.joining(delimiter));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return StringPool.EMPTY;
        }
        return collected;
    }

    /**
     * 快速读取文件内容
     *
     * @param fileName 文件名
     * @return 文件内容字符串
     */
    public static String fastReadFile(String fileName) {
        return fastReadFile(fileName, false);
    }

    /**
     * 将文件内容读取为单行字符串
     *
     * @param file 文件路径
     * @return 文件内容单行字符串
     */
    public static String readAsAline(String file) {
        return readAsAline(file, Symbol.EMPTY);
    }

    /**
     * 过滤目录下指定后缀的文件, 并查找包含目标字符串的文件
     *
     * @param directory  目录路径
     * @param fileSuffix 文件后缀 {@link FileSuffixConstants}
     * @param target     目标查找字符串
     * @throws RuntimeException 如果遍历文件时发生IO异常
     */
    public static void filterFileByTarget(String directory, String fileSuffix, String target) {
        try {
            Files.walkFileTree(Paths.get(directory), new FileVisitor<>() {
                @NotNull
                @Override
                public FileVisitResult preVisitDirectory(Path dir, @NotNull BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @NotNull
                @Override
                public FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(fileSuffix)) {
                        String context = Files.readString(file);
                        if (context.contains(target)) {
                            System.out.println(file);
                        }

                    }
                    return FileVisitResult.CONTINUE;
                }

                @NotNull
                @Override
                public FileVisitResult visitFileFailed(Path file, @NotNull IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @NotNull
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 读取文件为字节数组
     *
     * @param in 文件对象
     * @return 文件字节数组, 读取失败返回空数组
     */
    public static byte[] read(File in) {
        try {
            try (BufferedInputStream bf = new BufferedInputStream(new FileInputStream(in))) {
                byte[] data = new byte[bf.available()];
                int read = bf.read(data);
                log.info("文件 {} 大小为 {}", in.getAbsolutePath(), ByteUnitConverter.convert(read));
                return data;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return new byte[0];

    }

    /**
     * 读取文件为字节数组
     *
     * @param in 文件路径
     * @return 文件字节数组
     */
    public static byte[] read(String in) {
        return read(new File(in).getAbsoluteFile());
    }

    /**
     * 读取文件内容为字符串
     *
     * @param in 输入文件
     * @return 文件内容字符串, 读取失败返回空字符串
     */
    public String copyToString(File in) {
        try (FileInputStream fis = new FileInputStream(in);
             FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream()) {
            StreamUtils.copy(fis, outputStream);
            return outputStream.toString();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return StringUtils.EMPTY;

    }

    /**
     * 读取文件内容为字符串
     *
     * @param in 文件路径
     * @return 文件内容字符串
     * @throws IOException 如果读取文件失败
     */
    public static String copyToString(String in) throws IOException {
        return Files.readString(Paths.get(in));
    }

    /**
     * 复制文件
     *
     * @param in  源文件
     * @param out 目标文件
     */
    public void copy(File in, File out) {
        try (FileInputStream fis = new FileInputStream(in);
             FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream()) {
            StreamUtils.copy(fis, outputStream);
            Files.write(out.toPath(), outputStream.toByteArray());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 递归列出目录下所有文件
     *
     * @param path 要遍历的目录路径
     * @return 包含所有文件的List集合
     * @throws IOException 如果访问目录时发生IO错误
     */
    public static List<File> listFiles(String path) throws IOException {
        try (Stream<Path> stream = Files.walk(Paths.get(path))) {
            return stream.filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 使用递归方式列出目录下所有文件
     *
     * @param path 要遍历的目录路径
     * @return 包含所有文件的数组
     */
    public static File[] listFilesLoop(String path) {
        List<File> list = new ArrayList<>();
        Consumer<File> consumer = new Consumer<>() {
            @Override
            public void accept(File file) {
                if (file.isDirectory()) {
                    File[] children = file.listFiles();
                    if (children != null) {
                        for (File child : children) {
                            this.accept(child);
                        }
                    }
                } else {
                    list.add(file);
                }
            }
        };
        consumer.accept(new File(path));
        return list.toArray(new File[0]);
    }

    /**
     * 删除文件或目录(递归删除目录下所有内容)
     *
     * @param file 要删除的文件路径字符串
     */
    public static void delete(String file) {
        File dir = new File(file);
        delete(dir);
    }

    /**
     * 删除文件或目录(递归删除目录下所有内容)
     *
     * @param file 要删除的File对象
     */
    public static void delete(File file) {
        if (!file.exists()) {
            return;
        }
        Consumer<File> consumer = new Consumer<>() {
            @Override
            public void accept(File f) {
                String fp = f.getAbsoluteFile().toString();
                if (f.isDirectory()) {
                    File[] children = f.listFiles();
                    if (children != null) {
                        for (File child : children) {
                            this.accept(child);
                        }
                    }
                    // 删除空的文件夹
                    if (f.delete()) {
                        log.info("删除文件目录 ====> {}", fp);
                    }
                } else {
                    // 删除文件
                    if (f.delete()) {
                        log.info("删除文件 ====> {}", fp);
                    }
                }
            }
        };
        consumer.accept(file);
    }

    /**
     * 合并指定目录下所有指定后缀的文件到一个输出文件中
     *
     * @param sourceDir  源目录路径
     * @param outputFile 输出文件路径
     * @param fileSuffix 要合并的文件后缀 {@link FileSuffixConstants}
     * @throws IOException 如果目录不存在或合并过程中发生IO错误
     */
    private static void mergeFiles(String sourceDir, String outputFile, String fileSuffix) throws IOException {
        Path outputFilePath = Paths.get(outputFile);
        Path sourceDirPath = Paths.get(sourceDir);

        if (!Files.isDirectory(sourceDirPath)) {
            throw new IOException("指定的源目录不存在或不是一个目录: " + sourceDir);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputFilePath, StandardCharsets.UTF_8);
             Stream<Path> stream = Files.walk(sourceDirPath)) {
            stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(fileSuffix))
                    .forEach(path -> {
                        try {
                            writer.write(Files.readString(path));
                            writer.newLine();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }

    /**
     * 将文件以流的方式下载到客户端
     *
     * @param filePath 要下载的文件路径
     * @param request  HttpServletRequest对象
     * @param response HttpServletResponse对象
     * @throws BusinessException 如果文件不存在或写入流失败
     */

    public void downloadFileStream(String filePath, HttpServletRequest request, HttpServletResponse response) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new BusinessException("文件不存在");
        }
        String fileName = file.getName();
        response.setContentType(request.getServletContext().getMimeType(fileName));

        String encodeFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodeFileName + "\"");

        try (FileInputStream fis = new FileInputStream(file);
             ServletOutputStream writer = response.getOutputStream()) {
            IOUtils.copy(fis, writer);
            writer.flush();
        } catch (Exception ex) {
            throw new BusinessException("写入流失败");
        }
    }

    /**
     * 文件转换为base64
     * 将图片文件转化为字节数组字符串, 并对其进行Base64编码处理
     *
     * @param file 图片文件
     * @return String
     */
    public static String fileToBase64(File file) {
        // 对字节数组Base64编码并且去掉换行符(由于JDK自带base64不会去掉换行符,导致base64格式验证失败)
        // 根据RFC822规定, BASE64Encoder编码每76个字符, 还需要加上一个回车换行
        // 部分Base64编码的java库还按照这个标准实行
        return Base64.encodeBase64String(toByteArray(file));
    }

    /**
     * 创建目录(如果目录包含多个父层级则递归创建)
     *
     * @param path 要创建的目录路径
     * @throws RuntimeException 如果目录创建失败
     * @see org.apache.commons.io.FileUtils#forceMkdir
     * @see FileUtil#mkdir(String)
     */
    public static void createDir(String path) {
        var p = Paths.get(path);
        if (Files.exists(p)) {
            return;
        }
        Path dir;
        try {
            dir = Files.createDirectories(p);
            log.info("创建目录 {} 成功", dir.toFile().getAbsolutePath());
        } catch (IOException e) {
            log.error("创建目录失败, {}", e.getMessage(), e);
        }
    }

    /**
     * 在指定父目录下创建多个文件
     * {@link Files#createFile(Path, FileAttribute[])} 底层调用 WindowsNativeDispatcher#CreateFile0
     * 其中调用outputSteam写入文件时如果文件不存在也会调用CreateFile0创建文件
     *
     * @param parent    父目录路径
     * @param filenames 要创建的文件名数组
     * @throws RuntimeException 如果文件创建失败
     */
    public static void createFiles(String parent, String... filenames) {
        File pFile = new File(parent);
        if (!pFile.exists()) {
            createDir(parent);
        }
        for (String filename : filenames) {
            String path = parent + File.separator + filename;
            File file = new File(path);
            if (file.exists()) {
                continue;
            }
            boolean newFile = false;
            try {
                newFile = file.createNewFile();
            } catch (IOException e) {
                log.error("创建文件失败, {}", e.getMessage(), e);
            }
            if (newFile) {
                log.info("创建文件[{}]成功", file.getAbsolutePath());
            }
        }
    }

    /**
     * 创建文件
     *
     * @param out File out
     */
    public static void createFile(File out) {
        checkNotNull(out);
        try {
            if (!out.exists()) {
                Files.createFile(out.toPath());
            }
        } catch (IOException e) {
            log.error("创建文件 {} 失败, {}", out.getAbsolutePath(), e.getMessage(), e);
        }
    }

    /**
     * 向多个文件中填充文本内容
     *
     * @param parentDir  父目录路径
     * @param fileFilter 文件过滤器
     * @param size       目标文件大小(字节)
     * @param filenames  要填充的文件名数组
     */
    public static void fillTextToFile(String parentDir, Predicate<String> fileFilter, long size, String... filenames) {
        fillTextToFile(parentDir, fileFilter, size, 3, filenames);
    }

    /**
     * 向多个文件中填充文本内容
     *
     * @param parentDir  父目录路径
     * @param fileFilter 文件过滤器
     * @param size       目标文件大小(字节)
     * @param tryCount   网络请求重试次数
     * @param filenames  要填充的文件名数组
     */
    public static void fillTextToFile(String parentDir, Predicate<String> fileFilter, long size, final int tryCount, String... filenames) {
        createFiles(parentDir, filenames);
        List<String> toBeWriteFile = Arrays.stream(filenames).filter(fileFilter).toList();
        CountDownLatch latch = new CountDownLatch(toBeWriteFile.size());
        for (String fileName : toBeWriteFile) {
            pool.execute(() -> {
                String completeRelativePath = parentDir + File.separator + fileName;
                File file = new File(completeRelativePath);
                int tries = tryCount;
                String reviews = StringUtils.EMPTY;
                try {
                    do {
                        try {
                            reviews = NetworkUtils.fetchDailyRecommendedPoem().getReviews();
                        } catch (Exception e) {
                            log.warn("第 {} 次请求失败: {}", tryCount - tries + 1, e.getMessage());
                        }
                    } while (StringUtils.isEmpty(reviews) && --tries > 0);
                    if (StringUtils.isEmpty(reviews)) {
                        log.error("重试 {} 次后仍未获取到数据, 跳过文件 {}", tryCount, file.getAbsolutePath());
                        return;
                    }
                    log.info("写入文件 [{}] 开始, 准备写入数据 [{}]", file.getAbsolutePath(), StringUtils.abbreviate(reviews, 32));
                    fillTextToFile(completeRelativePath, reviews, size);
                    log.info("写入文件 [{}] 完成, 当前文件大小为 [{}] KB", file.getAbsolutePath(), file.length());
                } catch (Exception e) {
                    log.error("处理文件 {} 时异常: {}", file.getAbsolutePath(), e.getMessage(), e);
                } finally {
                    latch.countDown();
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        pool.shutdown();
    }

    /**
     * 向指定文件填充文本内容直到达到指定大小
     *
     * @param path 文件路径
     * @param text 要填充的文本内容
     * @param size 目标文件大小(字节)
     */
    public static void fillTextToFile(String path, String text, long size) {
        if (StringUtils.isEmpty(text) || size <= 0) {
            return;
        }
        try (FileWriter writer = new FileWriter(path)) {
            File file = new File(path);
            while (file.length() < size) {
                IOUtils.write(text.getBytes(Charset.forName("GBK")), writer, Charset.forName("GBK"));
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 合并文件夹下所有的文件内容到指定的文件
     *
     * @param in  文件夹路径
     * @param out 合并到指定的文件
     */
    public static void mergeFile(String in, String out) {
        mergeFile(in, out, MergeFileParam.create());
    }

    /**
     * 合并文件夹下所有的文件内容到指定的文件
     * 不使用多线程,性能提升不了太多且代码难以维护
     * 可以尝试使用AIO {@link AsynchronousFileChannel} 或者 NIO {@link FileChannel}
     * 或者先多线程读取到内存中再统一写入
     * 使用线程池 + 阻塞队列(Customer-Producer模式)
     *
     * @param in    文件夹路径
     * @param out   合并到指定的文件
     * @param param 合并时需要排除文件的后缀名称 [".txt", ".sql"],
     *              合并时需要排除的目录 [".git", "node_modules"]
     *              合并时需要排除的文件 ["index.js", "test.txt"]
     */
    public static void mergeFile(String in, String out, MergeFileParam param) {
        if (StringUtils.isEmpty(in)) {
            log.error("in 不能为空");
            return;
        }
        if (StringUtils.isEmpty(out)) {
            log.error("out 不能为空");
            return;
        }
        Path sourceDir = Paths.get(in);
        List<String> ignoredFileSuffixes = param.getIgnoredFileSuffixes();
        List<String> ignoredDirs = param.getIgnoredDirs();
        List<String> ignoreFiles = param.getIgnoredFiles();
        StopWatch sw = StopWatch.createStarted();
        File outParentFile = getParentFile(new File(out));
        createDir(outParentFile.toString());
        Path targetFile = Paths.get(out);
        try {
            try (Stream<Path> walk = Files.walk(sourceDir)) {
                TreeMap<Path, List<Path>> tree = walk
                        // 排除根目录
                        .filter(path -> !path.toFile().getAbsolutePath().equals(in))
                        // 排除不需要合并读取的文件夹
                        .filter(path -> !isExcludedDir(path, ignoredDirs))
                        // 排除不需要合并读取的文件
                        .filter(path -> !isExcludedFilename(path, ignoreFiles))
                        // 排除不需要合并读取的后缀文件
                        .filter(path -> !isExcludedFileSuffix(path, ignoredFileSuffixes))
                        .collect(Collectors.groupingBy(Path::getParent,
                                TreeMap::new, Collectors.toList()));
                try (BufferedWriter writer = Files.newBufferedWriter(targetFile, StandardCharsets.UTF_8)) {
                    int merged = 0;
                    log.info("文件写入合并开始");
                    File file;
                    for (Path kp : tree.keySet()) {
                        log.info("{}", kp.toFile().getAbsolutePath());
                        for (Path path : tree.get(kp)) {
                            file = path.toFile();
                            try {
                                if (Files.isRegularFile(path)) {
                                    if (isArchiveFile(file)) {
                                        handleArchiveFile(file, writer);
                                    } else if (isCompressFile(file)) {
                                        handleCompressFile(file, writer);
                                    } else {
                                        // 写入文件名
                                        String relativize = sourceDir.relativize(path).toString();
                                        writer.write("----------" + relativize + "----------");
                                        writer.newLine();

                                        // 写入文件内容
                                        // BufferedReader reader = Files.newBufferedReader(file)
                                        // BufferedReader 专门用于读取文本文件, 会尝试将文件内容按字符编码解析为字符串
                                        // 如果文件内容是二进制数据(如图片、音频、视频等), 使用 BufferedReader 读取时会抛出
                                        // MalformedInputException 或其他 IOException
                                        // 如果需要读取二进制文件,使用 FileInputStream 手动构造 BufferedReader
                                        try (BufferedReader reader = Files.newBufferedReader(path)) {
                                            String line;
                                            while ((line = reader.readLine()) != null) {
                                                writer.write(line);
                                                writer.newLine();
                                            }
                                        }
                                        writer.newLine();
                                        logFileInfo(path.getFileName().toString(), path.toFile().length());
                                    }
                                    merged++;
                                }
                            } catch (IOException e) {
                                log.error("文件 {} 写入合并异常, {}", path.getFileName(), e.getMessage());
                            }
                        }
                    }
                    log.info("文件写入合并结束, 本次一共合并 {} 个文件", merged);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    throw new RuntimeException(e);
                }
                // 确保文件写入操作完成后再获取文件大小(try块结束时,
                // BufferedWriter会被自动关闭,并触发close方法,确保
                // 所有缓冲区中的数据被写入到文件中,并且文件被正确关闭)
                log.info("合并后的文件大小为: {}", ByteUnitConverter.convert(targetFile.toFile().length()));
            }
            sw.stop();
            log.info("合并耗时: {}", TimeUnitConverter.convertMills(sw.getTime()));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private static void logFileInfo(String fileName, long size) {
        log.info("\t 文件名称: {} - 文件大小: {}", fileName, ByteUnitConverter.convert(size));
    }


    private static boolean isExcludedDir(Path path, List<String> ignoredDirs) {
        if (ignoredDirs == null) {
            return false;
        }
        // 实际使用时, ignoredDirs不会太多, 没必要使用Set
        for (String ignoredDir : ignoredDirs) {
            if (path.toString().contains(ignoredDir)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isExcludedFileSuffix(Path path, List<String> ignoredFileSuffixes) {
        if (ignoredFileSuffixes == null) {
            return false;
        }
        for (String ignoredFileSuffix : ignoredFileSuffixes) {
            if (path.toString().toLowerCase().endsWith(ignoredFileSuffix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isExcludedFilename(Path path, List<String> ignoreFiles) {
        if (ignoreFiles == null) {
            return false;
        }
        for (String ignoreFile : ignoreFiles) {
            if (StringUtil.equals(path.getFileName().toString(), ignoreFile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将文件内容读取到字节数组中
     *
     * @param in 读取的文件
     * @return 字节数组, 如果发生异常则返回空的字节数组
     * @see ByteStreams
     * @see com.google.common.io.Files#toByteArray(File)
     * @see MoreFiles
     * @see Resources
     */
    @SuppressWarnings("NullAway")
    public static byte[] toByteArray(File in) {
        checkNotNull(in);
        try (InputStream is = Files.newInputStream(in.toPath())) {
            return ByteStreams.toByteArray(is);
        } catch (IOException e) {
            log.error("Failed to read bytes from file: {}", in.getAbsolutePath(), e);
        }
        return new byte[0];
    }

    public static Path getRootpath(Path path) {
        if (path == null) {
            return null;
        }
        Path parent = path.getParent();
        if (parent == null) {
            return path;
        }
        return getRootpath(parent);
    }

    public static void copyfileByChannel(File in, File out) {
        checkNotNull(in);
        checkNotNull(out);
        // in out 必须存在
        if (!in.exists()) {
            log.error("文件 {} 不存在", in.getAbsolutePath());
            return;
        }
        File outParentFile = getParentFile(out);
        createDir(outParentFile.toString());
        createFile(out);
        try (FileChannel readChannel = FileChannel.open(in.toPath(), StandardOpenOption.READ);
             FileChannel writeChannel = FileChannel.open(out.toPath(), StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.allocate(Constants.KB);
            while (readChannel.read(buffer) != -1) {
                buffer.flip();
                writeChannel.write(buffer);
                buffer.clear();
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void copyfileByChannel(String in, String out) {
        copyfileByChannel(new File(in), new File(out));
    }

    public static File getParentFile(File in) {
        Objects.requireNonNull(in, "File in not null");
        File parentFile = in.getParentFile();
        // 只传入文件名找不到父级目录(因为无法从纯文件名中提取父目录), parentFile 可能为空
        if (parentFile == null) {
            // 处理绝对路径, 总能找到父目录
            Path parent = Paths.get(in.getAbsolutePath()).getParent();
            return parent.toFile();
        }
        return parentFile;
    }

    /**
     * 检查一个File是否是一个经过打包后的文件
     * Archive file: 通常指打包后的文件(如 .zip、.rar、.tar、.jar等格式),可能包含多个文件和文件夹
     * 读取文件的开头部分(签名)来确定文件类型
     * 如果文件的签名不匹配已知的归档文件类型, 抛出 {@link ArchiveException}
     *
     * @param file 需要检查的文件
     * @return 是否为 archive file
     */
    private static boolean isArchiveFile(File file) throws IOException {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }
        return isArchiveFile(new FileInputStream(file));
    }

    public static boolean isArchiveFile(InputStream is) throws IOException {
        // PPTX文件虽然本质上是ZIP格式, 但是使用ZipInputStream处理不了, 忽略
        byte[] original = IOUtils.toByteArray(is);
        if (isIgnoredFile(new ByteArrayInputStream(original))) {
            return false;
        }
        Set<String> localArchiveFileTypes = archiveFileTypes;
        try (BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(original))) {
            return localArchiveFileTypes.contains(ArchiveStreamFactory.detect(bis));
        } catch (IOException | ArchiveException e) {
            // No Archiver found for the stream signature, ignored
            return false;
        }
    }

    /**
     * 检查一个File是否是一个经过压缩后的文件
     *
     * @param file 需要检查的文件
     * @return 是否为 compress file
     * @see GZIPInputStream
     * @see #isArchiveFile
     */
    public static boolean isCompressFile(File file) throws FileNotFoundException {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }
        return isCompressFile(new FileInputStream(file));
    }

    public static boolean isCompressFile(InputStream is) {
        Set<String> localCompressFileTypes = compressFileTypes;
        try (BufferedInputStream bis = new BufferedInputStream(is)) {
            return localCompressFileTypes.contains(CompressorStreamFactory.detect(bis));
        } catch (IOException | CompressorException e) {
            // No Compressor found for the stream signature, ignored
            return false;
        }
    }

    private static void handleArchiveFile(File file, BufferedWriter writer) {
        log.info("文件 {} 是归档文件, 开始处理...", file.getAbsolutePath());
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
            log.info("归档文件写入合并开始");
            doWrite(new ZipArchiveReader(zis), null, writer);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private static void handleNestedArchiveFile(byte[] oba, BufferedWriter writer, String rootEntryName) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(oba))) {
            doWrite(new ZipArchiveReader(zis), rootEntryName, writer);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }


    private static void handleCompressFile(File file, BufferedWriter writer) {
        log.info("文件 {} 是压缩文件, 开始处理...", file.getAbsolutePath());
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis);
             GzipCompressorInputStream gzip = new GzipCompressorInputStream(bis);
             TarArchiveInputStream inputStream = new TarArchiveInputStream(gzip)) {
            log.info("压缩文件写入合并开始");
            doWrite(new TarArchiveReader(inputStream), null, writer);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private static void handleNestedCompressFile(byte[] oba, BufferedWriter writer, String rootEntryName) {
        // 先解压 GZIP 格式的数据流, 再读取 tar 归档文件
        // 顺序不可写反 .tar.gz -> GZIPInputStream -> TarArchiveInputStream -> 读取 tar entry 即 new TarArchiveInputStream(new GZIPInputStream(...))
        // 不能写成 new GZIPInputStream(new TarArchiveInputStream(...))
        try (TarArchiveInputStream inputStream = new TarArchiveInputStream
                (new GZIPInputStream(new ByteArrayInputStream(oba)))) {
            doWrite(new TarArchiveReader(inputStream), rootEntryName, writer);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private static void doWrite(ArchiveReader reader,
                                String rootEntryName,
                                BufferedWriter writer) throws IOException {
        ArchiveEntryWrapper entry;
        int entries = 0;
        byte[] original;
        String entryName;
        ByteArrayInputStream detectCompressFileStream = null;
        ByteArrayInputStream detectArchiveFileStream = null;
        ByteArrayInputStream detectIgnoredFileStream = null;
        ByteArrayInputStream writerInputStream = null;
        while ((entry = reader.getNextEntry()) != null) {
            if (entry.directory() || entry.size() == 0L) {
                continue;
            }
            if (rootEntryName != null) {
                // nested
                entryName = rootEntryName + IOUtils.DIR_SEPARATOR_UNIX + entry.name();
            } else {
                entryName = entry.name();
            }

            original = reader.readEntry();
            detectCompressFileStream = new ByteArrayInputStream(original);
            detectArchiveFileStream = new ByteArrayInputStream(original);
            detectIgnoredFileStream = new ByteArrayInputStream(original);
            if (isIgnoredFile(detectIgnoredFileStream)) {
                continue;
            }
            if (isCompressFile(detectCompressFileStream)) {
                log.info("嵌套的压缩文件 {}", entryName);
                handleNestedCompressFile(original, writer, entryName);
            } else if (isArchiveFile(detectArchiveFileStream)) {
                log.info("嵌套的归档文件 {}", entryName);
                handleNestedArchiveFile(original, writer, entryName);
            } else {
                writerInputStream = new ByteArrayInputStream(original);
                doWrite(writer, writerInputStream, entryName);
                // entry.size() = -1B?
                logFileInfo(entryName, original.length);
            }
            entries++;
        }
        log.info("写入合并结束, 实际读取合并共 {} 个文件", entries);
        try {
            IOUtils.close(detectCompressFileStream, detectArchiveFileStream, detectIgnoredFileStream, writerInputStream);
        } catch (IOExceptionList e) {
            log.error(e.getMessage(), e);
        }
    }


    private static void doWrite(BufferedWriter writer, InputStream inputStream, String entryName) throws IOException {
        // 写入文件名
        writer.write("----------" + entryName + "----------");
        writer.newLine();

        // 写入文件内容
        byte[] buffer = new byte[FileCopyUtils.BUFFER_SIZE];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            writer.write(new String(buffer, 0, length, StandardCharsets.UTF_8));
            writer.newLine();
        }
        writer.newLine();
    }

    /**
     * 使用apache.tika检测文件类型
     */
    public static boolean isIgnoredFile(InputStream inputStream) {
        try {
            if (inputStream == null || inputStream.available() == 0) {
                return false;
            }
            inputStream = FileMagic.prepareToCheckMagic(inputStream);
            inputStream.mark(512);
            FileMagic fileMagic = FileMagic.valueOf(inputStream);
            inputStream.reset();
            String mimeType = tika.detect(inputStream);
            if (fileMagic == FileMagic.OOXML) {
                // Java Archive(jar) 也是基于ZIP格式, 也会返回FileMagic.OOXML
                if (WellKnownMimeType.APPLICATION_ZIP.getString().equals(mimeType)) {
                    return false;
                }
                // 可能是PPTX, DOCX或XLSX(这几种文件也忽略)
                return "application/x-tika-ooxml"
                        .equals(mimeType);
            }
            return ignoredFileMagics.contains(fileMagic);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }
}
