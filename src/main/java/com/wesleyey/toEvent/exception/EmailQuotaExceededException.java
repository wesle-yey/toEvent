package com.wesleyey.toEvent.exception;

public class EmailQuotaExceededException extends RuntimeException {
    public EmailQuotaExceededException(String msg) { super(msg); }
}
