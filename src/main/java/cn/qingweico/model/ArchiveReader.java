package cn.qingweico.model;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author zqw
 * @date 2025/9/27
 */
public interface ArchiveReader extends Closeable {

    ArchiveEntryWrapper getNextEntry() throws IOException;
    byte[] readEntry() throws IOException;
    String name();
}
