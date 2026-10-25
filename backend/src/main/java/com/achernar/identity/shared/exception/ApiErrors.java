package com.achernar.identity.shared.exception;

import com.achernar.identity.profiles.exception.ProfileNotFoundException;
import com.achernar.identity.users.exception.UserNotFoundException;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> domain(ApiException e) {
        return ResponseEntity.status(e.status()).body(Map.of("code", e.status().name(), "message", e.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    ResponseEntity<?> userNotFound(UserNotFoundException e) {
        return notFound(e.getMessage());
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    ResponseEntity<?> profileNotFound(ProfileNotFoundException e) {
        return notFound(e.getMessage());
    }

    private ResponseEntity<?> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("code", "NOT_FOUND", "message", message));
    }

    @ExceptionHandler({ MethodArgumentNotValidException.class, HandlerMethodValidationException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
    ResponseEntity<?> invalid(Exception e) {
        return ResponseEntity.badRequest()
                .body(Map.of("code", "VALIDATION", "message", "Confira o formato e os limites dos campos informados."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception e) {
        org.slf4j.LoggerFactory.getLogger(ApiErrors.class).error("Falha interna: {}", e.getClass().getSimpleName());
        return ResponseEntity.internalServerError()
                .body(Map.of("code", "INTERNAL", "message", "Não foi possível concluir a operação."));
    }
}
