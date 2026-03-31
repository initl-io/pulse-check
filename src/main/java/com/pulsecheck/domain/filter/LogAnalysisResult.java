package com.pulsecheck.domain.filter;

import com.pulsecheck.domain.exception.ExceptionSummary;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LogAnalysisResult {
    private final List<String> lines;
    /** 각 line의 원본 파일 내 0-based 라인 번호 */
    private final List<Integer> lineNumbers;
    private final List<ExceptionSummary> exceptions;
    private final String startTime;
    private final String endTime;
}
