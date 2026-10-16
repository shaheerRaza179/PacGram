package com.pacgram.common.exception;

public class DuplicateException extends ConflictException {
    public DuplicateException(String errorCode, String message) {
        super(errorCode, message);
    }
}
