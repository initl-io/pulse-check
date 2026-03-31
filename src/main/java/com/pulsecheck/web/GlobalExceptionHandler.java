package com.pulsecheck.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Path Traversal 시도 등 보안 위반 → 403 */
    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleSecurity(SecurityException e) {
        return "ACCESS DENIED: " + e.getMessage();
    }

    /** 잘못된 경로·파라미터 → 400 */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadArg(IllegalArgumentException e) {
        return e.getMessage();
    }

    /** SSE 동시 접속 초과 → 429 */
    @ExceptionHandler(TooManyConnectionsException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public String handleQuota(TooManyConnectionsException e) {
        return e.getMessage();
    }

    /** 파일 IO 오류 → 500 */
    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleIo(IOException e) {
        log.error("File IO error", e);
        return "파일 읽기 오류: " + e.getMessage();
    }
}
