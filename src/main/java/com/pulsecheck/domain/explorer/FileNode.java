package com.pulsecheck.domain.explorer;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FileNode {
    public enum Type { FILE, DIRECTORY }

    private final String name;
    private final String path;
    private final Type type;
    private final long lastModified; // epoch millis
    private final List<FileNode> children;
}
