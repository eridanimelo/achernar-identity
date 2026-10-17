package com.eridani.achernar.identity.shared.web;

/**
 * Contexto da requisição corrente para auditoria e logs (RN-AUD-05). Não contém dados sensíveis.
 *
 * @param requestId identificador da requisição (também usado como {@code trace_id})
 * @param ip        IP do cliente resolvido por {@link TrustedProxyResolver} (pode ser nulo fora de requisição)
 * @param userAgent User-Agent truncado em 255 caracteres
 */
public record RequestInfo(String requestId, String ip, String userAgent) {

    public static final RequestInfo NONE = new RequestInfo(null, null, null);
}
