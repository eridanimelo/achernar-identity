package com.achernar.identity.security;

import com.achernar.identity.users.UserEntity;
import com.nimbusds.jose.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    private final RSAKey key;
    private final JwtEncoder encoder;
    private final String issuer;
    private final String audience;
    private final List<String> audiences;

    public TokenService(@Value("${app.key-file}") String path, @Value("${app.generate-key}") boolean generate,
            @Value("${app.issuer}") String issuer, @Value("${app.audience}") String audience,
            @Value("${app.additional-audiences:}") String additional) throws Exception {
        this.issuer = issuer;
        this.audience = audience;
        var configured = new ArrayList<String>();
        configured.add(audience);
        for (String value : additional.split(","))
            if (!value.isBlank())
                configured.add(value.strip());
        this.audiences = configured.stream().distinct().toList();
        Path file = Path.of(path);
        if (!Files.exists(file)) {
            if (!generate)
                throw new IllegalStateException("Chave de assinatura ausente; configure JWT_KEY_FILE.");
            Files.createDirectories(file.toAbsolutePath().getParent());
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(3072);
            KeyPair pair = generator.generateKeyPair();
            RSAKey created = new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .privateKey((RSAPrivateKey) pair.getPrivate()).keyID(UUID.randomUUID().toString()).build();
            Files.createFile(file, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            Files.writeString(file, created.toJSONString());
        }
        key = RSAKey.parse(Files.readString(file));
        if (!key.isPrivate() || key.size() < 2048)
            throw new IllegalStateException("Chave RSA inválida.");
        encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }

    public String issue(UserEntity user) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(issuer).audience(audiences).subject(user.id().toString())
                .issuedAt(now).expiresAt(now.plusSeconds(900)).id(UUID.randomUUID().toString())
                .claim("claim_version", 2).claim("roles", user.profiles()).claim("role", user.role())
                .claim("version", user.sessionVersion()).build();
        return encoder
                .encode(JwtEncoderParameters
                        .from(JwsHeader.with(SignatureAlgorithm.RS256).keyId(key.getKeyID()).build(), claims))
                .getTokenValue();
    }

    public JwtDecoder decoder() throws JOSEException {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256).build();
        decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtClaimValidator<List<String>>("aud", a -> a != null && a.contains(audience))));
        return decoder;
    }

    public Map<String, Object> publicKeys() {
        return new JWKSet(key.toPublicJWK()).toJSONObject();
    }
}
