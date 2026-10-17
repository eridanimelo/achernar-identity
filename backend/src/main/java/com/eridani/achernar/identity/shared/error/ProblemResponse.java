package com.eridani.achernar.identity.shared.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Corpo {@code application/problem+json} (RFC 9457 + extensões do catálogo). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemResponse(
        @JsonProperty("type") String type,
        @JsonProperty("title") String title,
        @JsonProperty("status") int status,
        @JsonProperty("code") String code,
        @JsonProperty("detail") String detail,
        @JsonProperty("instance") String instance,
        @JsonProperty("trace_id") String traceId,
        @JsonProperty("errors") List<FieldError> errors,
        @JsonProperty("retry_after_seconds") Long retryAfterSeconds) {
}
