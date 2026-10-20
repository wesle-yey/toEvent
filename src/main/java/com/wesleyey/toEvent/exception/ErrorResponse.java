package com.wesleyey.toEvent.exception;

import java.time.Instant;

public record ErrorResponse(
        int status,
        String message,
        Instant timestamp,
        String path) {

    public ErrorResponse(int status, String message) {
        this(status, message, Instant.now(), null);
    }

    public ErrorResponse withPath(String path) {
        return new ErrorResponse(this.status, this.message, this.timestamp, path);
    }
}
