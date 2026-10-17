package com.eridani.achernar.identity.shared.error;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exceção de domínio/aplicação traduzida para {@code application/problem+json} (D-32). */
public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final List<FieldError> errors;
    private final Long retryAfterSeconds;
    private final Map<String, String> headers = new LinkedHashMap<>();

    public ApiException(ErrorCode code) {
        this(code, null, List.of(), null, null);
    }

    public ApiException(ErrorCode code, String detail) {
        this(code, detail, List.of(), null, null);
    }

    public ApiException(ErrorCode code, List<FieldError> errors) {
        this(code, null, errors, null, null);
    }

    public ApiException(ErrorCode code, String detail, List<FieldError> errors, Long retryAfterSeconds, Throwable cause) {
        super(detail != null ? detail : code.code(), cause);
        this.code = code;
        this.errors = errors == null ? List.of() : List.copyOf(errors);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public static ApiException validation(String field, String code) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, List.of(FieldError.of(field, code)));
    }

    public static ApiException rateLimited(long retryAfterSeconds) {
        return new ApiException(ErrorCode.RATE_LIMITED, null, List.of(), Math.max(1, retryAfterSeconds), null);
    }

    /** Cabeçalho extra da resposta de erro (ex.: {@code Set-Cookie} de limpeza). */
    public ApiException withHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public Map<String, String> headers() {
        return Map.copyOf(headers);
    }

    public ErrorCode errorCode() {
        return code;
    }

    public List<FieldError> errors() {
        return errors;
    }

    public Long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
