package org.example.backend.common.exception;

import lombok.Getter;
import org.example.backend.common.model.BaseResponseStatus;

@Getter
public class BaseException extends RuntimeException {
    public BaseException(String message) {
        super(message);
    }

    public static BaseException from(BaseResponseStatus status) {
        return new BaseException(status.getMessage());
    }
}
