package com.pulsecheck.domain.filter;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LogContextResult {
    private final List<String> lines;
    /** 결과 첫 줄의 원본 파일 내 0-based 라인 번호 */
    private final int startLine;
    /** 더블클릭한 포커스 라인의 원본 파일 내 0-based 라인 번호 */
    private final int focusLine;
}
