package com.pulsecheck.domain.masking;

import lombok.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 로그 라인에서 민감 정보를 자동으로 마스킹한다.
 *
 * <p>지원 규칙:
 * <ul>
 *   <li>이메일 주소 → {@code u***@example.com}
 *   <li>한국 휴대전화 / 유선전화
 *   <li>신용카드 번호 패턴
 *   <li>API Key / Secret / Token (key=value 형식)
 *   <li>JWT Bearer 토큰
 *   <li>Private IP 주소 (10.x, 172.x, 192.168.x)
 * </ul>
 */
@Service
public class LogMaskingService {

    @Value
    private static class MaskRule {
        String name;
        Pattern pattern;
        String replacement;
    }

    private static final List<MaskRule> RULES = Arrays.asList(

        // 이메일: user 부분 일부만 남기고 마스킹
        new MaskRule("email",
            Pattern.compile("([a-zA-Z0-9])[a-zA-Z0-9._%+\\-]*@([a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,})"),
            "$1***@$2"),

        // 한국 전화번호 (010-1234-5678, 02-123-4567 등)
        new MaskRule("phone-kr",
            Pattern.compile("\\b(0\\d{1,2})[\\-.]?(\\d{3,4})[\\-.]?(\\d{4})\\b"),
            "$1-****-$3"),

        // 신용카드 (4자리 × 4 패턴)
        new MaskRule("credit-card",
            Pattern.compile("\\b(\\d{4})[\\s\\-]?(\\d{4})[\\s\\-]?(\\d{4})[\\s\\-]?(\\d{4})\\b"),
            "$1-****-****-$4"),

        // API Key / Secret / Token / Password = value 형식
        new MaskRule("api-key",
            Pattern.compile("(?i)(?:api_?key|access_?token|secret|bearer|password|passwd|pwd)" +
                            "\\s*[=:\\s]\\s*([\\w\\-\\.]{6})[\\w\\-\\.]+"),
            "[MASKED_KEY:$1***]"),

        // JWT 토큰 (eyJ... 형식)
        new MaskRule("jwt",
            Pattern.compile("eyJ[a-zA-Z0-9_\\-]{10,}\\.[a-zA-Z0-9_\\-]+\\.[a-zA-Z0-9_\\-]+"),
            "[MASKED_JWT]"),

        // Private IPv4 (내부망 IP)
        new MaskRule("private-ip",
            Pattern.compile("\\b(10\\.\\d{1,3}|172\\.(?:1[6-9]|2\\d|3[01])|192\\.168)" +
                            "\\.(\\d{1,3})\\.(\\d{1,3})\\b"),
            "$1.***.***")
    );

    /**
     * 한 라인에 모든 마스킹 규칙을 순차 적용한다.
     */
    public String mask(String line) {
        String result = line;
        for (MaskRule rule : RULES) {
            Matcher m = rule.getPattern().matcher(result);
            result = m.replaceAll(rule.getReplacement());
        }
        return result;
    }
}
