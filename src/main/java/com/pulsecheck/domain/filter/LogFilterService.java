package com.pulsecheck.domain.filter;

import com.pulsecheck.config.AppProperties;
import com.pulsecheck.domain.exception.ExceptionSummary;
import com.pulsecheck.domain.masking.LogMaskingService;
import com.pulsecheck.security.PathSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LogFilterService {

    private final AppProperties props;
    private final PathSecurityService pathSecurityService;
    private final LogMaskingService maskingService;

    private Pattern tsPattern;
    private Pattern msPattern;
    private Pattern exceptionPattern;

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TS_FMT_T = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @PostConstruct
    void compilePatterns() {
        AppProperties.Patterns p = props.getPatterns();
        tsPattern = Pattern.compile(p.getTimestamp());
        msPattern = Pattern.compile(p.getDurationMs());
        exceptionPattern = Pattern.compile(p.getException());
    }

    // ── 메인 분석 ───────────────────────────────────────────────

    public LogAnalysisResult analyze(LogFilterRequest req) throws IOException {
        Path safePath = pathSecurityService.validateAndResolve(req.getPath());

        LocalDateTime from = parseDateTime(req.getFrom());
        LocalDateTime to = parseDateTime(req.getTo());
        LocalTime fromTime = parseLocalTime(req.getFrom());
        LocalTime toTime = parseLocalTime(req.getTo());
        int limit = props.getMaxResultLines();

        List<String> filteredLines = new ArrayList<>();
        List<Integer> lineNumbers = new ArrayList<>();
        List<ExceptionSummary> exceptions = new ArrayList<>();
        ExceptionAccumulator accumulator = new ExceptionAccumulator();

        String firstTs = null;
        String lastTs = null;
        LocalDateTime lastParsedTs = null;

        try (BufferedReader reader = Files.newBufferedReader(safePath, StandardCharsets.UTF_8)) {
            String line;
            int lineIndex = 0;
            while ((line = reader.readLine()) != null) {
                accumulator.feed(line, exceptions);

                Matcher m = tsPattern.matcher(line);
                if (m.find()) {
                    String tsStr = m.group(1).trim();
                    if (firstTs == null)
                        firstTs = tsStr;
                    lastTs = tsStr;
                    lastParsedTs = parseTimestamp(tsStr, lastParsedTs);
                }

                boolean match = matchesTimeRange(lastParsedTs, from, to, fromTime, toTime);

                if (filteredLines.size() < limit
                        && matchesKeywords(line, req)
                        && matchesMs(line, req.getMinMs())
                        && match) {

                    filteredLines.add(req.isMasking() ? maskingService.mask(line) : line);
                    lineNumbers.add(lineIndex);
                }
                lineIndex++;
            }
            accumulator.flush(exceptions);
        }

        return LogAnalysisResult.builder()
                .lines(filteredLines)
                .lineNumbers(lineNumbers)
                .exceptions(exceptions)
                .startTime(firstTs)
                .endTime(lastTs)
                .build();
    }

    // ── 컨텍스트 조회 ──────────────────────────────────────────

    public LogContextResult getContext(String path, int focusLine, int context) throws IOException {
        Path safePath = pathSecurityService.validateAndResolve(path);
        int startLine = Math.max(0, focusLine - context);
        int endLine = focusLine + context;

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(safePath, StandardCharsets.UTF_8)) {
            String line;
            int idx = 0;
            while ((line = reader.readLine()) != null) {
                if (idx > endLine)
                    break;
                if (idx >= startLine)
                    lines.add(line);
                idx++;
            }
        }

        return LogContextResult.builder()
                .lines(lines)
                .startLine(startLine)
                .focusLine(focusLine)
                .build();
    }

    // ── 필터 메서드 ────────────────────────────────────────────

    private boolean matchesKeywords(String line, LogFilterRequest req) {
        List<String> kw = req.getKeywords();
        if (kw == null || kw.isEmpty())
            return true;
        String lower = line.toLowerCase();
        if ("OR".equalsIgnoreCase(req.getKeywordMode()))
            return kw.stream().anyMatch(k -> lower.contains(k.toLowerCase().trim()));
        return kw.stream().allMatch(k -> lower.contains(k.toLowerCase().trim()));
    }

    private boolean matchesMs(String line, Integer minMs) {
        if (minMs == null)
            return true;
        Matcher m = msPattern.matcher(line);
        if (!m.find())
            return false;
        return Integer.parseInt(m.group(1)) >= minMs;
    }

    private boolean matchesTimeRange(LocalDateTime ts, LocalDateTime from, LocalDateTime to, LocalTime fromTime,
            LocalTime toTime) {
        if (from == null && to == null && fromTime == null && toTime == null)
            return true;
        if (ts == null)
            return true;

        // If specific date/time was provided
        if (from != null && ts.isBefore(from))
            return false;
        if (to != null && ts.isAfter(to))
            return false;

        // If only time was provided (applies to each day)
        LocalTime tsTime = ts.toLocalTime();
        if (fromTime != null && tsTime.isBefore(fromTime))
            return false;
        if (toTime != null && tsTime.isAfter(toTime))
            return false;

        return true;
    }

    private LocalDateTime parseDateTime(String s) {
        if (!StringUtils.hasText(s) || isTimeOnly(s))
            return null;
        try {
            return LocalDateTime.parse(s);
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(s.replace("T", " "), TS_FMT);
        } catch (Exception ignored) {
        }
        return null;
    }

    private LocalTime parseLocalTime(String s) {
        if (!StringUtils.hasText(s) || !isTimeOnly(s))
            return null;
        try {
            String timePart = s.startsWith("T") ? s.substring(1) : s;
            return LocalTime.parse(timePart);
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean isTimeOnly(String s) {
        return s != null && (s.startsWith("T") || !s.contains("-"));
    }

    private LocalDateTime parseTimestamp(String s, LocalDateTime previous) {
        if (!StringUtils.hasText(s))
            return previous;
        // Date included? (YYYY-MM-DD...)
        if (s.contains("-")) {
            try {
                return LocalDateTime.parse(s, TS_FMT);
            } catch (Exception ignored) {
            }
            try {
                return LocalDateTime.parse(s, TS_FMT_T);
            } catch (Exception ignored) {
            }
        }
        // Time only? (HH:mm:ss.SSS)
        try {
            // Trim sub-seconds for standard parsing if needed, but LocalTime.parse often
            // handles it
            LocalTime lt = LocalTime.parse(s.contains(".") ? s.substring(0, s.indexOf(".")) : s);
            LocalDate d = (previous != null) ? previous.toLocalDate() : LocalDate.now();
            return LocalDateTime.of(d, lt);
        } catch (Exception ignored) {
        }
        return previous;
    }

    // ── Exception 상태 기계 ────────────────────────────────────

    private class ExceptionAccumulator {
        private String type, message, location;
        private StringBuilder stackTrace = new StringBuilder();
        private boolean active = false;

        void feed(String line, List<ExceptionSummary> out) {
            boolean isAt = line.startsWith("\tat ") || line.startsWith("at ");
            if (active && isAt) {
                stackTrace.append(line).append('\n');
                if (location == null)
                    location = line.trim().replaceFirst("^at ", "");
                return;
            }
            flush(out);
            Matcher m = exceptionPattern.matcher(line);
            if (m.find()) {
                type = m.group(1);
                message = m.group(2) != null ? m.group(2).trim() : "";
                location = null;
                stackTrace = new StringBuilder(line).append('\n');
                active = true;
            }
        }

        void flush(List<ExceptionSummary> out) {
            if (!active)
                return;
            out.add(ExceptionSummary.builder()
                    .type(type).message(message)
                    .location(location != null ? location : "")
                    .stackTrace(stackTrace.toString())
                    .build());
            active = false;
        }
    }
}
