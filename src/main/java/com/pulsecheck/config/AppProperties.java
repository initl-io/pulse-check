package com.pulsecheck.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pulsecheck")
public class AppProperties {

    @Value("${PULSECHECK_DEFAULT_LOG_PATH:${pulsecheck.default-log-path:${user.home}/logs}}")
    private String defaultLogPath;

    private int maxResultLines = 10_000;

    private Security security = new Security();
    private Sse sse = new Sse();
    private Patterns patterns = new Patterns();

    @Getter @Setter
    public static class Security {
        private String username = "admin";
        private String password = "pulsecheck!@#";
    }

    @Getter @Setter
    public static class Sse {
        private int maxConnections = 10;
        private long timeoutMinutes = 30;
    }

    @Getter @Setter
    public static class Patterns {
        private String timestamp  = "(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2})";
        private String level      = "\\b(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)\\b";
        private String durationMs = "\\((\\d{1,6})ms\\)";
        private String exception  = "([\\w.$]+Exception|[\\w.$]+Error)(?::\\s*(.*))?";
    }
}
