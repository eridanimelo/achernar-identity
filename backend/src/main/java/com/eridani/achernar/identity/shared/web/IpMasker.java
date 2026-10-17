package com.eridani.achernar.identity.shared.web;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/** IP aproximado para exibição/e-mail: IPv4 /24 e IPv6 /48 (RNF-15, FT-06). */
public final class IpMasker {

    private static final Pattern IP_LITERAL = Pattern.compile("^[0-9a-fA-F:.]{2,45}$");

    private IpMasker() {
    }

    public static String approximate(String ip) {
        if (ip == null || !IP_LITERAL.matcher(ip).matches()) {
            return null;
        }
        try {
            byte[] b = InetAddress.getByName(ip).getAddress();
            if (b.length == 4) {
                return (b[0] & 0xFF) + "." + (b[1] & 0xFF) + "." + (b[2] & 0xFF) + ".0/24";
            }
            if (b.length == 16) {
                return String.format("%x:%x:%x::/48",
                        ((b[0] & 0xFF) << 8) | (b[1] & 0xFF),
                        ((b[2] & 0xFF) << 8) | (b[3] & 0xFF),
                        ((b[4] & 0xFF) << 8) | (b[5] & 0xFF));
            }
            return null;
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
