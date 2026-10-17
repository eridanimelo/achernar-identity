package com.eridani.achernar.identity.shared.web;

/** Acesso ao {@link RequestInfo} da thread corrente (preenchido por {@link RequestIdFilter}). */
public final class RequestInfoHolder {

    private static final ThreadLocal<RequestInfo> CURRENT = new ThreadLocal<>();

    private RequestInfoHolder() {
    }

    public static RequestInfo get() {
        RequestInfo info = CURRENT.get();
        return info == null ? RequestInfo.NONE : info;
    }

    static void set(RequestInfo info) {
        CURRENT.set(info);
    }

    static void clear() {
        CURRENT.remove();
    }

    /** Executa um trecho com um contexto específico (jobs e testes). */
    public static void runWith(RequestInfo info, Runnable action) {
        RequestInfo previous = CURRENT.get();
        CURRENT.set(info);
        try {
            action.run();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
