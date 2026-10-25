package com.achernar.identity.shared.security;

import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
public class PublicEndpoints {
    private final TokenService tokens;

    public PublicEndpoints(TokenService tokens) {
        this.tokens = tokens;
    }

    @GetMapping("/api/csrf")
    Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @GetMapping("/api/health")
    Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/.well-known/jwks.json")
    Map<String, Object> jwks() {
        return tokens.publicKeys();
    }
}
