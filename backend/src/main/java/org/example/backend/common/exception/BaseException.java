package org.example.backend.common.exception;

import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {
    public BaseException(String message) {
        super(message);
    }
}
