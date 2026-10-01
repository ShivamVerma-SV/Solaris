package com.solaris.backend.exception;

public class ReportQueryException extends RuntimeException {
    public ReportQueryException(Throwable cause) {
        super("Unable to generate the requested report", cause);
    }
}
