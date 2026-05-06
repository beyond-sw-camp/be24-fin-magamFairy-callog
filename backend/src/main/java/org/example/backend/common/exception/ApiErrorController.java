package org.example.backend.common.exception;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.model.BaseResponseStatus;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<BaseResponse<?>> handleError(HttpServletRequest request) {
        HttpStatus status = resolveStatus(request);
        String message = resolveMessage(request, status);

        return ResponseEntity
                .status(status)
                .body(BaseResponse.fail(BaseResponseStatus.FAIL, message));
    }

    private HttpStatus resolveStatus(HttpServletRequest request) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusCode instanceof Integer code) {
            return HttpStatus.resolve(code) != null
                    ? HttpStatus.valueOf(code)
                    : HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveMessage(HttpServletRequest request, HttpStatus status) {
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        if (message instanceof String text && !text.isBlank()) {
            return text;
        }
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        if (exception instanceof Exception e && e.getMessage() != null && !e.getMessage().isBlank()) {
            return e.getMessage();
        }
        return status.getReasonPhrase();
    }
}
