package com.pulsecheck.domain.filter;

import lombok.Data;

import java.util.List;

@Data
public class LogFilterRequest {
    private String path;
    private List<String> keywords;
    private String keywordMode = "AND"; // AND | OR
    private Integer minMs;
    private String from; // ISO datetime
    private String to;   // ISO datetime
    private boolean masking = false; // PII 마스킹 여부
}
