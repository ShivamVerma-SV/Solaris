package com.solaris.backend.exception;

public class TokenStoreUnavailableException extends RuntimeException {
    public TokenStoreUnavailableException(Throwable cause) {
        super("Authentication token storage is temporarily unavailable", cause);
    }
}
