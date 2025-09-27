package cn.qingweico.model;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;

import java.io.IOException;

/**
 * @author zqw
 * @date 2025/9/27
 */
public class TarArchiveReader implements ArchiveReader {
    private final TarArchiveInputStream tis;

    public TarArchiveReader(TarArchiveInputStream tis) {
        this.tis = tis;
    }

    @Override
    public ArchiveEntryWrapper getNextEntry() throws IOException {
        TarArchiveEntry entry = tis.getNextEntry();
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
        return IOUtils.toByteArray(tis);
    }

    @Override
    public String name() {
        return "TarArchive";
    }

    @Override
    public void close() throws IOException {
        tis.close();
    }
}
