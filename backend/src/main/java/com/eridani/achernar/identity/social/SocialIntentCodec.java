package com.eridani.achernar.identity.social;

import com.eridani.achernar.identity.shared.crypto.MasterKeyCipher;
import com.eridani.achernar.identity.shared.json.MiniJson;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Intenção opaca dos fluxos LINK/REAUTH (FT-08): estado cifrado com a chave-mestra, sem persistência,
 * válido por {@value #TTL_SECONDS} s. O {@code start} a converte em uma {@link SocialTransaction}.
 */
@Component
public class SocialIntentCodec {

    public static final long TTL_SECONDS = 120;
    private static final String PURPOSE = "social-intent";

    public record Intent(Provider provider, SocialTransaction.Purpose purpose, UUID applicationId, String returnUrl,
                         String clientState, UUID accountId, UUID sessionId, Instant expiresAt) {
    }

    private final MasterKeyCipher cipher;
    private final Clock clock;

    public SocialIntentCodec(MasterKeyCipher cipher, Clock clock) {
        this.cipher = cipher;
        this.clock = clock;
    }

    public String encode(Provider provider, SocialTransaction.Purpose purpose, UUID applicationId, String returnUrl,
                         String clientState, UUID accountId, UUID sessionId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("p", provider.name());
        m.put("u", purpose.name());
        m.put("a", applicationId.toString());
        m.put("r", returnUrl);
        if (clientState != null) {
            m.put("s", clientState);
        }
        m.put("ac", accountId.toString());
        m.put("si", sessionId.toString());
        m.put("e", clock.instant().plus(Duration.ofSeconds(TTL_SECONDS)).getEpochSecond());
        byte[] blob = cipher.encryptString(MiniJson.write(m), PURPOSE);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(blob);
    }

    public Optional<Intent> decode(String token) {
        if (token == null || token.isBlank() || token.length() > 2048) {
            return Optional.empty();
        }
        try {
            byte[] blob = Base64.getUrlDecoder().decode(token.getBytes(StandardCharsets.US_ASCII));
            Map<String, Object> m = MiniJson.parseObject(cipher.decryptString(blob, PURPOSE));
            Instant exp = Instant.ofEpochSecond(((Number) m.get("e")).longValue());
            if (!exp.isAfter(clock.instant())) {
                return Optional.empty();
            }
            return Optional.of(new Intent(Provider.valueOf((String) m.get("p")),
                    SocialTransaction.Purpose.valueOf((String) m.get("u")), UUID.fromString((String) m.get("a")),
                    (String) m.get("r"), (String) m.get("s"), UUID.fromString((String) m.get("ac")),
                    UUID.fromString((String) m.get("si")), exp));
        } catch (RuntimeException e) {
            return Optional.empty(); // adulterado, expirado ou malformado
        }
    }
}
