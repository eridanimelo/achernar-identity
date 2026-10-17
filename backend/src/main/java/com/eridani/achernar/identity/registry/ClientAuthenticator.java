package com.eridani.achernar.identity.registry;

import com.eridani.achernar.identity.shared.crypto.ConstantTime;
import com.eridani.achernar.identity.shared.crypto.Sha256;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import com.eridani.achernar.identity.shared.web.BasicAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Identifica e autentica a aplicação cliente de uma requisição {@code /auth/**} (D-08, RN-AUT-04, RN-APP-05):
 * {@code BROWSER} por {@code client_id} + {@code Origin}; {@code CONFIDENTIAL} por HTTP Basic.
 */
@Service
public class ClientAuthenticator {

    private final ApplicationRepository apps;
    private final Clock clock;

    public ClientAuthenticator(ApplicationRepository apps, Clock clock) {
        this.apps = apps;
        this.clock = clock;
    }

    /**
     * @param bodyClientId {@code client_id} do corpo ou da query (pode ser nulo se houver Basic)
     * @param requireOrigin exige {@code Origin} permitido para aplicações {@code BROWSER}
     */
    public ClientContext authenticate(HttpServletRequest request, String bodyClientId, boolean requireOrigin) {
        Optional<BasicAuth.Credentials> basic = BasicAuth.parse(request.getHeader("Authorization"));
        String clientId = bodyClientId;
        if (clientId == null || clientId.isBlank()) {
            clientId = basic.map(BasicAuth.Credentials::id).orElse(null);
        } else if (basic.isPresent() && !basic.get().id().equals(clientId)) {
            throw new ApiException(ErrorCode.INVALID_CLIENT);
        }
        if (clientId == null || clientId.isBlank()) {
            throw ApiException.validation("client_id", "REQUIRED");
        }
        Application app = apps.findByClientId(clientId).orElse(null);
        if (app == null || !app.active()) {
            // Tempo equivalente ao de uma verificação de segredo (não diferenciar desconhecido de inativo).
            basic.ifPresent(b -> ConstantTime.equals(Sha256.hex(b.secret()), "x"));
            throw new ApiException(ErrorCode.INVALID_CLIENT);
        }
        if (app.type() == ClientType.CONFIDENTIAL) {
            String secret = basic.map(BasicAuth.Credentials::secret).orElse(null);
            if (secret == null || !secretValid(app, secret)) {
                throw new ApiException(ErrorCode.INVALID_CLIENT);
            }
            return new ClientContext(app, true);
        }
        if (requireOrigin) {
            String origin = request.getHeader("Origin");
            if (origin == null || !app.hasOrigin(origin)) {
                throw new ApiException(ErrorCode.ORIGIN_NOT_ALLOWED);
            }
        }
        return new ClientContext(app, false);
    }

    private boolean secretValid(Application app, String secret) {
        String hash = Sha256.hex(secret);
        boolean match = false;
        for (String stored : apps.activeSecretHashes(app.id())) {
            match |= ConstantTime.equals(stored, hash);
        }
        if (match) {
            apps.touchSecret(app.id(), hash, clock.instant());
        }
        return match;
    }
}
