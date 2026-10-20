package com.wesleyey.toEvent.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // fallback caso não seja capturada por um handler
public final class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException() { super(); }
    public UserAlreadyExistsException(String msg) { super(msg); }
}
