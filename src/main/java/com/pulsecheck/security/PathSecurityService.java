package com.pulsecheck.security;

import com.pulsecheck.config.AppProperties;
import com.pulsecheck.domain.explorer.AllowedRoot;
import com.pulsecheck.domain.explorer.AllowedRootRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Path Traversal 방어 전담 서비스.
 *
 * <p>모든 파일 접근 전에 반드시 {@link #validateAndResolve(String)}를 호출해야 한다.
 * <ul>
 *   <li>{@code Path.toAbsolutePath().normalize()} 로 {@code ../../} 등 상위 디렉터리 탈출을 무력화
 *   <li>정규화된 경로가 DB에 등록된 허용 루트의 하위인지 {@code startsWith()} 로 화이트리스트 검증
 *   <li>허용되지 않은 경로 접근은 예외를 던지고 WARN 로그를 남긴다
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PathSecurityService {

    private final AllowedRootRepository allowedRootRepo;
    private final AppProperties props;

    // ── 경로 검증 ──────────────────────────────────────────────

    /**
     * 입력 경로를 정규화하고 허용된 루트 내인지 검증한다.
     *
     * @param inputPath 사용자/클라이언트가 전달한 원시 경로 문자열
     * @return 정규화된 절대 경로
     * @throws SecurityException 허용 범위를 벗어난 경로 접근 시 (Path Traversal 시도 포함)
     */
    public Path validateAndResolve(String inputPath) {
        Path normalized = resolveInternal(inputPath);

        List<Path> allowedRoots = getEffectiveRoots();

        boolean permitted = allowedRoots.stream()
                .anyMatch(root -> normalized.startsWith(root));

        if (!permitted) {
            log.warn("[SECURITY] Path traversal attempt BLOCKED | input='{}' normalized='{}' allowedRoots={}",
                    inputPath, normalized, allowedRoots);
            throw new SecurityException(
                    "허용되지 않은 경로입니다. (시도: " + normalized + ")");
        }

        return normalized;
    }

    // ── 루트 등록 ──────────────────────────────────────────────

    public Path registerRoot(String inputPath) {
        Path normalized = resolveInternal(inputPath);
        File dir = normalized.toFile();

        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException("유효하지 않은 디렉터리: " + normalized);
        }

        List<Path> existing = getEffectiveRoots();
        boolean alreadyCovered = existing.stream()
                .anyMatch(r -> normalized.startsWith(r));
        if (alreadyCovered) {
            log.debug("Path already covered by existing root: {}", normalized);
            return normalized;
        }

        if (!allowedRootRepo.existsByPath(normalized.toString())) {
            allowedRootRepo.save(new AllowedRoot(normalized.toString()));
            log.info("[SECURITY] New root registered: {}", normalized);
        }

        return normalized;
    }

    private Path resolveInternal(String inputPath) {
        if (!StringUtils.hasText(inputPath)) {
            return Paths.get(props.getDefaultLogPath()).toAbsolutePath().normalize();
        }
        Path p = Paths.get(inputPath);
        if (!p.isAbsolute()) {
            p = Paths.get(props.getDefaultLogPath(), inputPath);
        }
        return p.toAbsolutePath().normalize();
    }

    public List<AllowedRoot> getRegisteredRoots() {
        return allowedRootRepo.findAll();
    }

    public void removeRoot(Long id) {
        allowedRootRepo.deleteById(id);
        log.info("[SECURITY] Root removed: id={}", id);
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────

    /**
     * DB에 등록된 경로 + 설정 기본값을 합쳐서 실제로 존재하는 경로만 반환.
     */
    private List<Path> getEffectiveRoots() {
        // 1. DB에 등록된 루트들
        List<Path> roots = allowedRootRepo.findAll().stream()
                .map(r -> Paths.get(r.getPath()).toAbsolutePath().normalize())
                .filter(p -> p.toFile().exists())
                .collect(Collectors.toList());

        // 2. application.yml(.env)의 defaultLogPath를 항상 허용 목록에 추가
        String defLogPath = props.getDefaultLogPath();
        if (StringUtils.hasText(defLogPath)) {
            Path def = Paths.get(defLogPath).toAbsolutePath().normalize();
            if (def.toFile().exists() && !roots.contains(def)) {
                roots.add(def);
            }
        }
        
        return roots;
    }
}
