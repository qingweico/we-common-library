package cn.qingweico.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * @author zqw
 * @date 2025/9/8
 */
@Getter
public class MergeFileParam {
    private final List<String> ignoredFileSuffixes;
    private final List<String> ignoredDirs;
    private final List<String> ignoredFiles;

    private MergeFileParam(Builder builder) {
        this.ignoredFileSuffixes = builder.ignoredFileSuffixes;
        this.ignoredDirs = builder.ignoredDirs;
        this.ignoredFiles = builder.ignoredFiles;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MergeFileParam create() {
        return builder().build();
    }

    public static class Builder {
        private List<String> ignoredFileSuffixes;
        private List<String> ignoredDirs;
        private List<String> ignoredFiles;

        public Builder ignoredFileSuffixes(String... ignoredFileSuffixes) {
            this.ignoredFileSuffixes = Arrays.asList(ignoredFileSuffixes);
            return this;
        }

        public Builder ignoredDirs(String... ignoredDirs) {
            this.ignoredDirs = Arrays.asList(ignoredDirs);
            return this;
        }

        public Builder ignoreFiles(String... ignoredFiles) {
            this.ignoredFiles = Arrays.asList(ignoredFiles);
            return this;
        }

        public MergeFileParam build() {
            return new MergeFileParam(this);
        }
    }
}
