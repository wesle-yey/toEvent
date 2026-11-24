package com.wesleyey.toEvent.exception;

public class VerificationEmailCooldownException extends RuntimeException {
    private final long retryAfterSeconds;

    public VerificationEmailCooldownException(String msg, long retryAfterSeconds) {
        super(msg);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
