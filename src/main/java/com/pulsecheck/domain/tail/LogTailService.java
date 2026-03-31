package com.pulsecheck.domain.tail;

import com.pulsecheck.config.AppProperties;
import com.pulsecheck.domain.masking.LogMaskingService;
import com.pulsecheck.security.PathSecurityService;
import com.pulsecheck.web.TooManyConnectionsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogTailService {

    private static final int POLL_MS = 500;

    private final AppProperties       props;
    private final PathSecurityService pathSecurityService;
    private final LogMaskingService   maskingService;

    /** 현재 활성 SSE 연결 수 */
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(
                    Runtime.getRuntime().availableProcessors(),
                    r -> { Thread t = new Thread(r, "tail-worker"); t.setDaemon(true); return t; });

    /**
     * @throws TooManyConnectionsException 동시 접속 한도 초과
     * @throws SecurityException           허용되지 않은 경로
     */
    public SseEmitter tail(String rawPath, boolean masking) {
        AppProperties.Sse sseConf = props.getSse();
        int maxConn = sseConf.getMaxConnections();

        // ① 동시 접속 쿼터 검사
        if (activeConnections.get() >= maxConn) {
            throw new TooManyConnectionsException(
                    "SSE 동시 접속 한도(" + maxConn + "개)를 초과했습니다. 잠시 후 시도하세요.");
        }

        // ② Path Traversal 검증
        Path safePath = pathSecurityService.validateAndResolve(rawPath);
        String path   = safePath.toString();

        long timeoutMs = sseConf.getTimeoutMinutes() * 60 * 1000L;
        SseEmitter emitter = new SseEmitter(timeoutMs);

        activeConnections.incrementAndGet();
        log.info("[SSE] Connection opened: path={} active={}", path, activeConnections.get());

        ScheduledFuture<?>[] taskRef  = new ScheduledFuture<?>[1];
        long[]               lastSize = { fileLength(path) };

        Runnable poll = () -> {
            try {
                long current = fileLength(path);
                if (current > lastSize[0]) {
                    // ③ 새로 추가된 바이트만 읽기 (RandomAccessFile)
                    try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
                        raf.seek(lastSize[0]);
                        String line;
                        while ((line = raf.readLine()) != null) {
                            // ISO-8859-1로 읽힌 바이트를 UTF-8로 재해석
                            String decoded = new String(line.getBytes("ISO-8859-1"), "UTF-8");
                            String output  = masking ? maskingService.mask(decoded) : decoded;
                            emitter.send(SseEmitter.event().data(output));
                        }
                        lastSize[0] = raf.getFilePointer();
                    }
                } else {
                    // 변경 없으면 keep-alive ping
                    emitter.send(SseEmitter.event().name("ping").data("PING"));
                }
            } catch (IOException e) {
                emitter.completeWithError(e);
                cancel(taskRef[0]);
            }
        };

        taskRef[0] = scheduler.scheduleAtFixedRate(poll, 0, POLL_MS, TimeUnit.MILLISECONDS);

        // ④ 연결 종료 시 반드시 카운터 감소
        Runnable cleanup = () -> {
            cancel(taskRef[0]);
            int remaining = activeConnections.decrementAndGet();
            log.info("[SSE] Connection closed: path={} active={}", path, remaining);
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        return emitter;
    }

    private long fileLength(String path) {
        File f = new File(path);
        return f.exists() ? f.length() : 0;
    }

    private void cancel(ScheduledFuture<?> task) {
        if (task != null && !task.isCancelled()) task.cancel(false);
    }

    /** 현재 활성 연결 수 (모니터링용) */
    public int getActiveConnections() {
        return activeConnections.get();
    }
}
