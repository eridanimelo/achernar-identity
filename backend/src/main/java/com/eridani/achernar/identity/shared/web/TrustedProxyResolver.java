package com.eridani.achernar.identity.shared.web;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Resolve o IP real do cliente (P-47, SEC-22). {@code X-Forwarded-For} só é considerado quando a conexão vem de
 * um proxy confiável (lista de CIDR); caso contrário vale o endereço remoto da conexão.
 */
public final class TrustedProxyResolver {

    private static final Pattern IP_LITERAL = Pattern.compile("^[0-9a-fA-F:.]{2,45}$");

    private record Cidr(byte[] network, int prefixLength) {
        boolean contains(byte[] address) {
            if (address.length != network.length) {
                return false;
            }
            int fullBytes = prefixLength / 8;
            for (int i = 0; i < fullBytes; i++) {
                if (address[i] != network[i]) {
                    return false;
                }
            }
            int remaining = prefixLength % 8;
            if (remaining == 0) {
                return true;
            }
            int mask = 0xFF << (8 - remaining) & 0xFF;
            return (address[fullBytes] & mask) == (network[fullBytes] & mask);
        }
    }

    private final List<Cidr> trusted = new ArrayList<>();

    public TrustedProxyResolver(List<String> cidrs) {
        if (cidrs != null) {
            for (String c : cidrs) {
                if (c != null && !c.isBlank()) {
                    trusted.add(parseCidr(c.trim()));
                }
            }
        }
    }

    public String resolve(String remoteAddr, String forwardedFor) {
        String remote = literalOrNull(remoteAddr);
        if (remote == null) {
            return null;
        }
        if (trusted.isEmpty() || !isTrusted(remote) || forwardedFor == null || forwardedFor.isBlank()) {
            return remote;
        }
        String[] hops = forwardedFor.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String hop = literalOrNull(hops[i].trim());
            if (hop == null) {
                return remote;
            }
            if (!isTrusted(hop)) {
                return hop;
            }
        }
        return remote;
    }

    public boolean isTrusted(String ip) {
        byte[] bytes = toBytes(ip);
        if (bytes == null) {
            return false;
        }
        for (Cidr c : trusted) {
            if (c.contains(bytes)) {
                return true;
            }
        }
        return false;
    }

    private static Cidr parseCidr(String text) {
        String[] parts = text.split("/");
        byte[] net = toBytes(parts[0]);
        if (net == null) {
            throw new IllegalArgumentException("CIDR inválido em ACHERNAR_TRUSTED_PROXIES: " + text);
        }
        int max = net.length * 8;
        int prefix = parts.length > 1 ? Integer.parseInt(parts[1]) : max;
        if (prefix < 0 || prefix > max) {
            throw new IllegalArgumentException("Prefixo CIDR inválido: " + text);
        }
        return new Cidr(net, prefix);
    }

    private static String literalOrNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (!IP_LITERAL.matcher(t).matches() || toBytes(t) == null) {
            return null;
        }
        return t;
    }

    /** Converte somente literais de IP (nunca consulta DNS). */
    static byte[] toBytes(String ip) {
        if (ip == null || !IP_LITERAL.matcher(ip).matches()) {
            return null;
        }
        try {
            return InetAddress.getByName(ip).getAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
