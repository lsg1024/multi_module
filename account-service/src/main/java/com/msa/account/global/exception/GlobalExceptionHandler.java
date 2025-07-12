package com.msa.account.global.exception;

import com.msacommon.global.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<String>> handleGenericException(IllegalArgumentException e) {

        ApiResponse<String> body = ApiResponse.error(e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleUserNotFoundException(NotFoundException e) {
        ApiResponse<String> body = ApiResponse.error(e.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        ApiResponse<Map<String, String>> body = ApiResponse.error(errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }

}
