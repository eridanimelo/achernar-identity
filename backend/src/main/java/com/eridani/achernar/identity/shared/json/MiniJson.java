package com.eridani.achernar.identity.shared.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON mínimo (escrita e leitura de objetos simples) para campos {@code text} do banco (payload do outbox,
 * {@code details} de eventos, JWK público). Sem dependência de ORM/Jackson para persistência.
 */
public final class MiniJson {

    private MiniJson() {
    }

    // ---------------------------------------------------------------- escrita
    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object v) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String s) {
            writeString(sb, s);
        } else if (v instanceof Boolean || v instanceof Integer || v instanceof Long || v instanceof Short) {
            sb.append(v);
        } else if (v instanceof Number n) {
            sb.append(n);
        } else if (v instanceof Map<?, ?> m) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(sb, String.valueOf(e.getKey()));
                sb.append(':');
                writeValue(sb, e.getValue());
            }
            sb.append('}');
        } else if (v instanceof Iterable<?> it) {
            sb.append('[');
            boolean first = true;
            for (Object o : it) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeValue(sb, o);
            }
            sb.append(']');
        } else {
            writeString(sb, v.toString());
        }
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    // ---------------------------------------------------------------- leitura
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Object v = new Parser(json).parseDocument();
        if (!(v instanceof Map)) {
            throw new IllegalArgumentException("JSON não é um objeto");
        }
        return (Map<String, Object>) v;
    }

    private static final class Parser {
        private final String s;
        private int i;

        Parser(String s) {
            this.s = s == null ? "" : s;
        }

        Object parseDocument() {
            skipWs();
            Object v = value();
            skipWs();
            if (i != s.length()) {
                throw error("conteúdo após o fim do documento");
            }
            return v;
        }

        private Object value() {
            skipWs();
            if (i >= s.length()) {
                throw error("fim inesperado");
            }
            char c = s.charAt(i);
            return switch (c) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++;
            skipWs();
            if (peek() == '}') {
                i++;
                return m;
            }
            while (true) {
                skipWs();
                String k = string();
                skipWs();
                expect(':');
                m.put(k, value());
                skipWs();
                char c = next();
                if (c == '}') {
                    return m;
                }
                if (c != ',') {
                    throw error("esperado ',' ou '}'");
                }
            }
        }

        private List<Object> array() {
            List<Object> l = new ArrayList<>();
            i++;
            skipWs();
            if (peek() == ']') {
                i++;
                return l;
            }
            while (true) {
                l.add(value());
                skipWs();
                char c = next();
                if (c == ']') {
                    return l;
                }
                if (c != ',') {
                    throw error("esperado ',' ou ']'");
                }
            }
        }

        private String string() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (i >= s.length()) {
                    throw error("string não terminada");
                }
                char c = s.charAt(i++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    char e = next();
                    switch (e) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (i + 4 > s.length()) {
                                throw error("escape unicode incompleto");
                            }
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                        }
                        default -> throw error("escape inválido");
                    }
                } else {
                    sb.append(c);
                }
            }
        }

        private Object number() {
            int start = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) {
                i++;
            }
            String t = s.substring(start, i);
            if (t.isEmpty()) {
                throw error("valor inválido");
            }
            if (t.contains(".") || t.contains("e") || t.contains("E")) {
                return Double.parseDouble(t);
            }
            return Long.parseLong(t);
        }

        private Object literal(String text, Object value) {
            if (!s.startsWith(text, i)) {
                throw error("literal inválido");
            }
            i += text.length();
            return value;
        }

        private void skipWs() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }

        private char peek() {
            return i < s.length() ? s.charAt(i) : '\0';
        }

        private char next() {
            if (i >= s.length()) {
                throw error("fim inesperado");
            }
            return s.charAt(i++);
        }

        private void expect(char c) {
            if (next() != c) {
                throw error("esperado '" + c + "'");
            }
        }

        private IllegalArgumentException error(String msg) {
            return new IllegalArgumentException("JSON inválido na posição " + i + ": " + msg);
        }
    }
}
