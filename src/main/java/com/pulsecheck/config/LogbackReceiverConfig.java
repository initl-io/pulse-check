package com.pulsecheck.config;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.server.ServerSocketReceiver;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogbackReceiverConfig {

    private static final int RECEIVER_PORT = 4560;
    private ServerSocketReceiver receiver;

    @PostConstruct
    public void startLogbackReceiver() {
        try {
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

            // Register a StatusListener to see internal Logback errors
            lc.getStatusManager().add(new StatusListener() {
                @Override
                public void addStatusEvent(Status status) {
                    if (status.getEffectiveLevel() >= Status.WARN) {
                        System.err.println("Logback Status: " + status.getMessage());
                        if (status.getThrowable() != null) {
                            status.getThrowable().printStackTrace();
                        }
                    } else {
                        System.err.print("else !");
                    }
                }
            });

            receiver = new ServerSocketReceiver();
            receiver.setContext(lc);
            receiver.setPort(RECEIVER_PORT);
            receiver.start();

            System.out.println("Started Logback ServerSocketReceiver on port " + RECEIVER_PORT);
        } catch (Exception e) {
            System.err.println("Failed to start Logback ServerSocketReceiver: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void stopLogbackReceiver() {
        if (receiver != null && receiver.isStarted()) {
            receiver.stop();
            System.out.println("Stopped Logback ServerSocketReceiver on port " + RECEIVER_PORT);
        }
    }
}
