package com.pulsecheck.domain.exception;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExceptionSummary {
    private final String type;
    private final String message;
    private final String location;
    private final String stackTrace;
}
