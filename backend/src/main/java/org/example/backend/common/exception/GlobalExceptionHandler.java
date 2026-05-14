package org.example.backend.common.exception;

import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.model.BaseResponseStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<BaseResponse<Void>> handleBaseException(BaseException exception) {
        return validationError(exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BaseResponse<Void>> handleIllegalArgumentException(IllegalArgumentException exception) {
        return validationError(exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<BaseResponse<Void>> handleDataIntegrityViolationException() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorResponse("이미 사용 중인 값입니다."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<BaseResponse<Void>> handleResponseStatusException(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        return ResponseEntity.status(status != null ? status : HttpStatus.BAD_REQUEST)
                .body(errorResponse(exception.getReason()));
    }

    private ResponseEntity<BaseResponse<Void>> validationError(String message) {
        return ResponseEntity.badRequest().body(errorResponse(message));
    }

    private BaseResponse<Void> errorResponse(String message) {
        String resolvedMessage = message == null || message.isBlank()
                ? BaseResponseStatus.VALIDATION_ERROR.getMessage()
                : message;
        return new BaseResponse<>(
                BaseResponseStatus.VALIDATION_ERROR.isSuccess(),
                BaseResponseStatus.VALIDATION_ERROR.getCode(),
                resolvedMessage,
                null
        );
    }
}
