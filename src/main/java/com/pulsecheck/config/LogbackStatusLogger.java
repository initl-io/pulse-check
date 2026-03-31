package com.pulsecheck.config;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Logback의 내부 상태(Status)를 감시하여 
 * 클라이언트의 Socket 연결/해제 이벤트를 애플리케이션 로그에 기록하는 컴포넌트입니다.
 */
@Slf4j
@Component
public class LogbackStatusLogger implements StatusListener {

    @PostConstruct
    public void init() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getStatusManager().add(this);
        log.info("Logback Status Listener registered to capture connection events.");
    }

    @Override
    public void addStatusEvent(Status status) {
        String msg = status.getMessage();
        if (msg == null) return;

        if (status.getLevel() >= Status.WARN) {
            log.warn("[Logback Error] {}", msg);
            if (status.getThrowable() != null) {
                log.error("Exception details:", status.getThrowable());
            }
        } else {
            log.info("[Logback Status] {}", msg);
        }
    }
}
