package cn.qingweico.model;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author zqw
 * @date 2025/9/27
 */
public class ZipArchiveReader implements ArchiveReader {
    private final ZipInputStream zis;

    public ZipArchiveReader(ZipInputStream zis) {
        this.zis = zis;
    }

    @Override
    public ArchiveEntryWrapper getNextEntry() throws IOException {
        ZipEntry entry = zis.getNextEntry();
        if (entry == null) {
            return null;
        }
        return new ArchiveEntryWrapper(
                entry.getName(),
                entry.isDirectory(),
                entry.getSize()
        );
    }

    @Override
    public byte[] readEntry() throws IOException {
        return IOUtils.toByteArray(zis);
    }

    @Override
    public String name() {
        return "ZipArchive";
    }

    @Override
    public void close() throws IOException {
        zis.close();
    }
}
