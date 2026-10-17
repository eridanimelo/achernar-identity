package com.eridani.achernar.identity.shared.error;

import com.eridani.achernar.identity.shared.config.AchernarProperties;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/** Monta {@link ProblemResponse} com títulos/detalhes localizados (D-37) e {@code type} derivado do issuer. */
@Component
public class ProblemFactory {

    private final MessageSource messages;
    private final AchernarProperties props;

    public ProblemFactory(MessageSource messages, AchernarProperties props) {
        this.messages = messages;
        this.props = props;
    }

    public ProblemResponse build(ApiException ex, String instance, String traceId, Locale locale) {
        return build(ex.errorCode(), explicitDetail(ex), ex.errors(), ex.retryAfterSeconds(), instance, traceId, locale);
    }

    public ProblemResponse build(ErrorCode code, String detail, List<FieldError> errors, Long retryAfter,
                                 String instance, String traceId, Locale locale) {
        String title = messages.getMessage("problem." + code.code() + ".title", null, code.code(), locale);
        String resolvedDetail = detail != null ? detail
                : messages.getMessage("problem." + code.code() + ".detail", null, null, locale);
        List<FieldError> localized = new ArrayList<>();
        for (FieldError fe : errors == null ? List.<FieldError>of() : errors) {
            String msg = fe.message() != null ? fe.message()
                    : messages.getMessage("field." + fe.code(), new Object[] {passwordMin()}, null, locale);
            localized.add(new FieldError(fe.field(), fe.code(), msg));
        }
        return new ProblemResponse(
                props.issuerBase() + "/problems/" + code.typeSlug(),
                title,
                code.status(),
                code.code(),
                resolvedDetail,
                instance,
                traceId,
                localized.isEmpty() ? null : localized,
                retryAfter);
    }

    private int passwordMin() {
        return props.password().minLength();
    }

    private static String explicitDetail(ApiException ex) {
        // ApiException guarda o código como mensagem quando não há detalhe explícito.
        return ex.getMessage() == null || ex.getMessage().equals(ex.errorCode().code()) ? null : ex.getMessage();
    }

    /** Serialização manual (filtros e handlers de segurança, fora do MVC). */
    public static Map<String, Object> toMap(ProblemResponse p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", p.type());
        m.put("title", p.title());
        m.put("status", p.status());
        m.put("code", p.code());
        if (p.detail() != null) {
            m.put("detail", p.detail());
        }
        if (p.instance() != null) {
            m.put("instance", p.instance());
        }
        if (p.traceId() != null) {
            m.put("trace_id", p.traceId());
        }
        if (p.errors() != null) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (FieldError e : p.errors()) {
                Map<String, Object> em = new LinkedHashMap<>();
                em.put("field", e.field());
                em.put("code", e.code());
                if (e.message() != null) {
                    em.put("message", e.message());
                }
                list.add(em);
            }
            m.put("errors", list);
        }
        if (p.retryAfterSeconds() != null) {
            m.put("retry_after_seconds", p.retryAfterSeconds());
        }
        return m;
    }
}
