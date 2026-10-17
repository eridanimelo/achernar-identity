package com.eridani.achernar.identity.shared.error;

import com.eridani.achernar.identity.shared.json.MiniJson;
import com.eridani.achernar.identity.shared.web.LocaleSupport;
import com.eridani.achernar.identity.shared.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;

/** Escreve {@code application/problem+json} fora do MVC (filtros, entry points, access denied handlers). */
@Component
public class ProblemWriter {

    private final ProblemFactory factory;

    public ProblemWriter(ProblemFactory factory) {
        this.factory = factory;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code) throws IOException {
        write(request, response, code, null, List.of(), null);
    }

    public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code, String detail,
                      List<FieldError> errors, Long retryAfter) throws IOException {
        String traceId = (String) request.getAttribute(RequestIdFilter.ATTRIBUTE);
        String lang = LocaleSupport.resolve(request.getHeader("Accept-Language"), LocaleSupport.PT_BR);
        ProblemResponse p = factory.build(code, detail, errors, retryAfter, request.getRequestURI(), traceId,
                LocaleSupport.toJavaLocale(lang));
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(code.status());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store");
        if (retryAfter != null) {
            response.setHeader("Retry-After", String.valueOf(retryAfter));
        }
        response.getWriter().write(MiniJson.write(ProblemFactory.toMap(p)));
        response.getWriter().flush();
    }
}
