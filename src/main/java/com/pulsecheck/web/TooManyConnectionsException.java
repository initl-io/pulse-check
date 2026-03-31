package com.pulsecheck.web;

public class TooManyConnectionsException extends RuntimeException {
    public TooManyConnectionsException(String message) {
        super(message);
    }
}
