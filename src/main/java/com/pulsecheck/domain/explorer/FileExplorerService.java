package com.pulsecheck.domain.explorer;

import com.pulsecheck.security.PathSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileExplorerService {

    private static final Set<String> ALLOWED_EXTENSIONS = 
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(".log", ".txt")));

    private final PathSecurityService pathSecurityService;
    private final com.pulsecheck.config.AppProperties appProperties;

    /**
     * 루트 경로를 검증·등록하고 파일 트리를 반환한다.
     * 경로가 없으면 .env/설정의 default-log-path를 사용한다.
     */
    public FileNode getFileTree(String rootPath) {
        String baseStr = appProperties.getDefaultLogPath();
        String targetPath = !StringUtils.hasText(rootPath) ? baseStr : rootPath;

        log.debug("log path => {}", targetPath);
        Path resolvedRoot = pathSecurityService.registerRoot(targetPath);
        Path base = Paths.get(baseStr).toAbsolutePath().normalize();

        return buildNode(resolvedRoot.toFile(), base);
    }

    private FileNode buildNode(File file, Path base) {
        String relativePath = base.relativize(file.toPath().toAbsolutePath().normalize()).toString();
        // 비어있으면(루트인 경우) 점(.) 등으로 표시하기보다 빈 문자열 또는 본래 이름을 유지
        if (relativePath.isEmpty()) relativePath = "";

        if (!file.isDirectory()) {
            if (!hasAllowedExtension(file.getName()))
                return null;
            return FileNode.builder()
                    .name(file.getName())
                    .path(relativePath)
                    .type(FileNode.Type.FILE)
                    .lastModified(file.lastModified())
                    .children(Collections.emptyList())
                    .build();
        }

        List<FileNode> children = new java.util.ArrayList<>();
        collectFiles(file, children, base);

        children.sort((a, b) -> Long.compare(b.getLastModified(), a.getLastModified()));

        return FileNode.builder()
                .name(file.getName())
                .path(relativePath)
                .type(FileNode.Type.DIRECTORY)
                .lastModified(file.lastModified())
                .children(children)
                .build();
    }

    private void collectFiles(File dir, List<FileNode> acc, Path base) {
        File[] entries = dir.listFiles();
        if (entries == null)
            return;

        for (File entry : entries) {
            if (entry.isDirectory()) {
                collectFiles(entry, acc, base);
            } else {
                if (hasAllowedExtension(entry.getName())) {
                    String rel = base.relativize(entry.toPath().toAbsolutePath().normalize()).toString();
                    acc.add(FileNode.builder()
                            .name(entry.getName())
                            .path(rel)
                            .type(FileNode.Type.FILE)
                            .lastModified(entry.lastModified())
                            .children(Collections.emptyList())
                            .build());
                }
            }
        }
    }

    private boolean hasAllowedExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0)
            return false;
        return ALLOWED_EXTENSIONS.contains(filename.substring(dot).toLowerCase());
    }
}
