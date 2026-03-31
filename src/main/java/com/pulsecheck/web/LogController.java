package com.pulsecheck.web;

import com.pulsecheck.domain.filter.LogAnalysisResult;
import com.pulsecheck.domain.filter.LogContextResult;
import com.pulsecheck.domain.filter.LogFilterRequest;
import com.pulsecheck.domain.filter.LogFilterService;
import com.pulsecheck.domain.tail.LogTailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.springframework.util.StringUtils;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LogController {

    private final LogFilterService logFilterService;
    private final LogTailService   logTailService;

    @GetMapping("/log")
    public ResponseEntity<?> getLog(
            @RequestParam String path,
            @RequestParam(required = false) String keywords,
            @RequestParam(defaultValue = "AND") String keywordMode,
            @RequestParam(required = false) Integer minMs,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "false") boolean masking) throws Exception {

        LogFilterRequest req = new LogFilterRequest();
        req.setPath(path);
        req.setKeywordMode(keywordMode);
        req.setMinMs(minMs);
        req.setFrom(from);
        req.setTo(to);
        req.setMasking(masking);

        if (StringUtils.hasText(keywords)) {
            List<String> kw = Arrays.stream(keywords.split(","))
                    .map(String::trim)
                    .filter(k -> !k.isEmpty())
                    .collect(Collectors.toList());
            req.setKeywords(kw);
        }

        LogAnalysisResult result = logFilterService.analyze(req);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/log/context")
    public ResponseEntity<?> getContext(
            @RequestParam String path,
            @RequestParam int line,
            @RequestParam(defaultValue = "20") int context) throws Exception {

        LogContextResult result = logFilterService.getContext(path, line, context);
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/tail", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter tailLog(
            @RequestParam String path,
            @RequestParam(defaultValue = "false") boolean masking) {

        return logTailService.tail(path, masking);
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(
                java.util.Collections.singletonMap("activeTailConnections", logTailService.getActiveConnections()));
    }
}
