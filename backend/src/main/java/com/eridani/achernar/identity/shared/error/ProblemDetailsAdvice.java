package com.eridani.achernar.identity.shared.error;

import com.eridani.achernar.identity.shared.web.BodyTooLargeException;
import com.eridani.achernar.identity.shared.web.LocaleSupport;
import com.eridani.achernar.identity.shared.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Tradução central de exceções para {@code application/problem+json} (D-32, SEC-37). */
@RestControllerAdvice
public class ProblemDetailsAdvice {

    private static final Logger LOG = LoggerFactory.getLogger(ProblemDetailsAdvice.class);
    private static final MediaType PROBLEM = MediaType.parseMediaType("application/problem+json");

    private final ProblemFactory factory;

    public ProblemDetailsAdvice(ProblemFactory factory) {
        this.factory = factory;
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemResponse> api(ApiException ex, HttpServletRequest req) {
        return respond(ex.errorCode(), null, ex.errors(), ex.retryAfterSeconds(), req, ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemResponse> invalidBody(MethodArgumentNotValidException ex, HttpServletRequest req) {
        return respond(ErrorCode.VALIDATION_ERROR, null, fromBinding(ex), null, req, null);
    }

    @ExceptionHandler(BindException.class)
    ResponseEntity<ProblemResponse> bind(BindException ex, HttpServletRequest req) {
        return respond(ErrorCode.VALIDATION_ERROR, null, fromBinding(ex), null, req, null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemResponse> constraint(ConstraintViolationException ex, HttpServletRequest req) {
        List<FieldError> errors = new ArrayList<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            String path = v.getPropertyPath().toString();
            errors.add(FieldError.of(snake(path.substring(path.lastIndexOf('.') + 1)), "INVALID"));
        }
        return respond(ErrorCode.VALIDATION_ERROR, null, errors, null, req, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ProblemResponse> missingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return respond(ErrorCode.VALIDATION_ERROR, null,
                List.of(FieldError.of(snake(ex.getParameterName()), "REQUIRED")), null, req, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemResponse> typeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return respond(ErrorCode.VALIDATION_ERROR, null,
                List.of(FieldError.of(snake(ex.getName()), "INVALID_FORMAT")), null, req, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemResponse> unreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof BodyTooLargeException) {
                return respond(ErrorCode.PAYLOAD_TOO_LARGE, null, List.of(), null, req, null);
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return respond(ErrorCode.MALFORMED_REQUEST, null, List.of(), null, req, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemResponse> denied(AccessDeniedException ex, HttpServletRequest req) {
        return respond(ErrorCode.FORBIDDEN, null, List.of(), null, req, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemResponse> unauthenticated(AuthenticationException ex, HttpServletRequest req) {
        return respond(ErrorCode.UNAUTHENTICATED, null, List.of(), null, req, null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemResponse> fallback(Exception ex, HttpServletRequest req) {
        if (ex instanceof ErrorResponse er) {
            int status = er.getStatusCode().value();
            ErrorCode code = switch (status) {
                case 404 -> ErrorCode.NOT_FOUND;
                case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
                case 413 -> ErrorCode.PAYLOAD_TOO_LARGE;
                case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
                default -> status >= 500 ? ErrorCode.INTERNAL_ERROR : ErrorCode.MALFORMED_REQUEST;
            };
            ResponseEntity<ProblemResponse> base = respond(code, null, List.of(), null, req, null);
            HttpHeaders extra = er.getHeaders();
            if (extra == null || extra.isEmpty()) {
                return base;
            }
            HttpHeaders merged = new HttpHeaders();
            merged.putAll(base.getHeaders());
            extra.forEach((k, v) -> {
                if (!HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(k)) {
                    merged.put(k, v);
                }
            });
            return new ResponseEntity<>(base.getBody(), merged, base.getStatusCode());
        }
        String traceId = (String) req.getAttribute(RequestIdFilter.ATTRIBUTE);
        LOG.error("Erro inesperado trace_id={} tipo={}", traceId, ex.getClass().getName(), ex);
        return respond(ErrorCode.INTERNAL_ERROR, null, List.of(), null, req, null);
    }

    // ---------------------------------------------------------------- util
    private ResponseEntity<ProblemResponse> respond(ErrorCode code, String detail, List<FieldError> errors,
                                                    Long retryAfter, HttpServletRequest req, ApiException source) {
        String traceId = (String) req.getAttribute(RequestIdFilter.ATTRIBUTE);
        Locale locale = LocaleSupport.toJavaLocale(
                LocaleSupport.resolve(req.getHeader("Accept-Language"), LocaleSupport.PT_BR));
        ProblemResponse body = source != null
                ? factory.build(source, req.getRequestURI(), traceId, locale)
                : factory.build(code, detail, errors, retryAfter, req.getRequestURI(), traceId, locale);
        ResponseEntity.BodyBuilder b = ResponseEntity.status(code.status())
                .contentType(PROBLEM)
                .header(HttpHeaders.CACHE_CONTROL, "no-store");
        if (body.retryAfterSeconds() != null) {
            b.header(HttpHeaders.RETRY_AFTER, String.valueOf(body.retryAfterSeconds()));
        }
        if (source != null) {
            source.headers().forEach(b::header);
        }
        return b.body(body);
    }

    private static List<FieldError> fromBinding(BindException ex) {
        List<FieldError> out = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> out.add(FieldError.of(snake(fe.getField()), codeFor(fe.getCode()))));
        ex.getBindingResult().getGlobalErrors().forEach(ge -> out.add(FieldError.of("_", codeFor(ge.getCode()))));
        return out;
    }

    private static String codeFor(String constraint) {
        if (constraint == null) {
            return "INVALID";
        }
        return switch (constraint) {
            case "NotBlank", "NotNull", "NotEmpty" -> "REQUIRED";
            case "Size", "Length" -> "TOO_LONG";
            case "Email", "Pattern" -> "INVALID_FORMAT";
            case "Min", "Max", "Range" -> "OUT_OF_RANGE";
            default -> "INVALID";
        };
    }

    /** {@code clientId} → {@code client_id}; {@code items[0].audience} → {@code items[0].audience}. */
    static String snake(String camel) {
        if (camel == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
