package com.achernar.identity.security;
import com.achernar.identity.users.UserLookup;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8(); }
    @Bean JwtDecoder jwtDecoder(TokenService tokens) throws Exception { return tokens.decoder(); }
    @Bean SecurityFilterChain chain(HttpSecurity http, UserLookup users, @Value("${app.secure-cookie}") boolean secure) throws Exception {
        var csrf=CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookieCustomizer(cookie -> cookie.sameSite("Strict").path("/").secure(secure));
        var handler=new CsrfTokenRequestAttributeHandler();
        handler.setCsrfRequestAttributeName(null);
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(c -> c.csrfTokenRepository(csrf).csrfTokenRequestHandler(handler).ignoringRequestMatchers(request -> request.getHeader("Authorization")!=null && request.getHeader("Authorization").startsWith("Bearer ") && !request.getRequestURI().startsWith("/api/auth/")))
            .authorizeHttpRequests(a -> a.requestMatchers("/api/csrf","/api/auth/register","/api/auth/login","/api/auth/verify",
                "/api/auth/resend","/api/auth/forgot-password","/api/auth/reset-password","/api/auth/activate","/api/auth/token","/api/health","/.well-known/jwks.json").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN").anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.bearerTokenResolver(request -> {
                String uri=request.getRequestURI();
                if(request.getHeader("Authorization")!=null)return new org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver().resolve(request);
                if(uri.equals("/api/csrf") || (uri.startsWith("/api/auth/") && !uri.equals("/api/auth/logout") && !uri.equals("/api/auth/session"))) return null;
                var cookies=request.getCookies(); if(cookies==null) return null;
                for(var cookie:cookies) if(cookie.getName().equals("IDENTITY_SESSION")) return cookie.getValue();
                return null;
            }).jwt(j -> j.jwtAuthenticationConverter(jwt -> {
                try {
                    var user=users.byId(UUID.fromString(jwt.getSubject())).orElseThrow(SecurityConfig::invalidSession);
                    Number version=jwt.getClaim("version");
                    if(!user.verified() || !user.status().equals("ACTIVE") || version==null || user.sessionVersion()!=version.longValue()
                        || !user.profiles().equals(jwt.getClaimAsStringList("roles")) || !(jwt.getClaim("claim_version") instanceof Number cv && cv.intValue()==2)) throw invalidSession();
                    return new JwtAuthenticationToken(jwt,user.profiles().stream().map(code->new SimpleGrantedAuthority("ROLE_"+code)).toList(),user.id().toString());
                } catch(IllegalArgumentException e) { throw invalidSession(); }
            })).authenticationEntryPoint((req,res,e) -> { res.setStatus(401); res.setContentType("application/json"); res.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Entre novamente para continuar.\"}"); }))
            .exceptionHandling(e -> e.authenticationEntryPoint((req,res,ex) -> {res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Autenticação necessária.\"}");})
                .accessDeniedHandler((req,res,ex) -> {res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"Acesso não permitido. Atualize a página e tente novamente.\"}");}))
            .headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives("default-src 'none'; frame-ancestors 'none'")));
        return http.build();
    }
    private static OAuth2AuthenticationException invalidSession() { return new OAuth2AuthenticationException(new OAuth2Error("invalid_token"),"Sessão inválida."); }
}
