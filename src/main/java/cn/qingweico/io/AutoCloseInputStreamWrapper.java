package cn.qingweico.io;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author zqw
 * @date 2025/7/26
 */
public class AutoCloseInputStreamWrapper extends InputStream {
    Logger logger = LoggerFactory.getLogger(AutoCloseInputStreamWrapper.class);

    InputStream stream;
    Closeable[] closeable;

    public AutoCloseInputStreamWrapper(InputStream stream, Closeable... closeable) {
        this.stream = stream;
        this.closeable = closeable;
    }

    @Override
    public int read() throws IOException {
        return stream.read();
    }

    @Override
    public int read(@NotNull byte[] b) throws IOException {
        return stream.read(b);
    }

    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        return stream.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return super.skip(n);
    }

    @Override
    public int available() throws IOException {
        return stream.available();
    }

    @Override
    public void close() throws IOException {
        try {
            stream.close();
        } catch (Exception e) {
            logger.warn("输入流关闭失败", e);
        }
        if (closeable != null) {
            for (Closeable c : closeable) {
                try {
                    c.close();
                } catch (Exception e) {
                    logger.warn("输入流关闭失败", e);
                }
            }
        }
    }

    @Override
    public synchronized void mark(int readLimit) {
        stream.mark(readLimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        stream.reset();
    }

    @Override
    public boolean markSupported() {
        return stream.markSupported();
    }
}
