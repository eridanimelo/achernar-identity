package com.achernar.identity.shared;

import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> domain(ApiException e) {
        return ResponseEntity.status(e.status()).body(Map.of("code", e.status().name(), "message", e.getMessage()));
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
