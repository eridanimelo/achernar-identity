package com.eridani.achernar.identity.security;

import com.eridani.achernar.identity.registry.DynamicCorsConfigurationSource;
import com.eridani.achernar.identity.session.SessionService;
import com.eridani.achernar.identity.shared.config.AchernarProperties;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import com.eridani.achernar.identity.shared.error.ProblemWriter;
import com.eridani.achernar.identity.shared.web.BodySizeLimitFilter;
import com.eridani.achernar.identity.shared.web.RequestIdFilter;
import com.eridani.achernar.identity.shared.web.SecurityHeadersFilter;
import com.eridani.achernar.identity.shared.web.TrustedProxyResolver;
import com.eridani.achernar.identity.token.AccessTokenVerifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * Cadeias de segurança (SEC-*): API stateless, sem sessão HTTP, sem CSRF por cookie (a defesa do refresh é o
 * cabeçalho {@code X-Achernar-Client} + Origin), CORS dinâmico e erros em {@code application/problem+json}.
 */
@Configuration
public class SecurityConfig {

    // ------------------------------------------------------------------ filtros de borda (antes do Spring Security)
    @Bean
    FilterRegistrationBean<RequestIdFilter> requestIdFilter(TrustedProxyResolver resolver) {
        FilterRegistrationBean<RequestIdFilter> b = new FilterRegistrationBean<>(new RequestIdFilter(resolver));
        b.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return b;
    }

    @Bean
    FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter(AchernarProperties props) {
        FilterRegistrationBean<SecurityHeadersFilter> b = new FilterRegistrationBean<>(
                new SecurityHeadersFilter(props.issuer() != null && props.issuer().startsWith("https://")));
        b.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return b;
    }

    @Bean
    FilterRegistrationBean<BodySizeLimitFilter> bodySizeLimitFilter(AchernarProperties props, ProblemWriter problems) {
        FilterRegistrationBean<BodySizeLimitFilter> b = new FilterRegistrationBean<>(
                new BodySizeLimitFilter(props.maxBodyBytes(), problems));
        b.setOrder(Ordered.HIGHEST_PRECEDENCE + 3);
        return b;
    }

    // ------------------------------------------------------------------ cadeias
    @Bean
    @Order(1)
    SecurityFilterChain actuatorChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/actuator/**")
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return stateless(http).build();
    }

    /** Introspecção: servidor→servidor, autenticada por Basic no próprio controlador; sem CORS. */
    @Bean
    @Order(2)
    SecurityFilterChain introspectionChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/v1/tokens/introspect")
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return stateless(http).build();
    }

    /** Rotas públicas: autenticação da aplicação cliente (client_id/Basic) é feita nos controladores. */
    @Bean
    @Order(3)
    SecurityFilterChain publicChain(HttpSecurity http, DynamicCorsConfigurationSource cors) throws Exception {
        http.securityMatcher("/auth/**", "/setup/**", "/.well-known/**")
                .cors(c -> c.configurationSource(cors))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return stateless(http).build();
    }

    @Bean
    @Order(4)
    SecurityFilterChain apiChain(HttpSecurity http, DynamicCorsConfigurationSource cors, AccessTokenVerifier verifier,
                                 SessionService sessions, AchernarProperties props, ProblemWriter problems)
            throws Exception {
        http.securityMatcher("/api/**")
                .cors(c -> c.configurationSource(cors))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new BearerTokenFilter(verifier, sessions, props, problems), AuthorizationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> {
                            res.setHeader("WWW-Authenticate", "Bearer");
                            problems.write(req, res, ErrorCode.UNAUTHENTICATED);
                        })
                        .accessDeniedHandler((req, res, ex) -> problems.write(req, res, ErrorCode.FORBIDDEN)));
        return stateless(http).build();
    }

    /** Todo o restante é negado (nada exposto por omissão). */
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    SecurityFilterChain defaultChain(HttpSecurity http, ProblemWriter problems) throws Exception {
        http.authorizeHttpRequests(a -> a.anyRequest().denyAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> problems.write(req, res, ErrorCode.NOT_FOUND))
                        .accessDeniedHandler((req, res, ex) -> problems.write(req, res, ErrorCode.NOT_FOUND)));
        return stateless(http).build();
    }

    private static HttpSecurity stateless(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(f -> f.deny()))
                .anonymous(Customizer.withDefaults());
    }
}
