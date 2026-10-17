package com.eridani.achernar.identity.social;

import com.eridani.achernar.identity.auth.TokenResponses;
import com.eridani.achernar.identity.registry.ClientAuthenticator;
import com.eridani.achernar.identity.registry.ClientContext;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import com.eridani.achernar.identity.shared.web.LocaleSupport;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** FT-07: API-FT07-01…03. */
@RestController
@RequestMapping("/auth/social")
public class SocialController {

    public record ExchangeRequest(@JsonProperty("client_id") String clientId, @JsonProperty("code") String code,
                                  @JsonProperty("code_verifier") String codeVerifier,
                                  @JsonProperty("redirect_uri") String redirectUri) {
    }

    private final SocialLoginService service;
    private final ClientAuthenticator clients;

    public SocialController(SocialLoginService service, ClientAuthenticator clients) {
        this.service = service;
        this.clients = clients;
    }

    @GetMapping("/{provider}/start")
    public ResponseEntity<Void> start(@PathVariable("provider") String slug,
                                      @RequestParam(value = "client_id", required = false) String clientId,
                                      @RequestParam(value = "redirect_uri", required = false) String redirectUri,
                                      @RequestParam(value = "state", required = false) String state,
                                      @RequestParam(value = "code_challenge", required = false) String codeChallenge,
                                      @RequestParam(value = "code_challenge_method", required = false) String method,
                                      @RequestParam(value = "audiences", required = false) String audiences,
                                      @RequestParam(value = "intent", required = false) String intent) {
        Provider provider = Provider.fromSlug(slug).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        SocialLoginService.Redirect r = service.start(provider, new SocialLoginService.StartParams(clientId,
                redirectUri, state, codeChallenge, method, audiences, intent));
        return redirect(r);
    }

    @GetMapping("/{provider}/callback")
    public ResponseEntity<Void> callback(HttpServletRequest request, @PathVariable("provider") String slug,
                                         @RequestParam(value = "code", required = false) String code,
                                         @RequestParam(value = "state", required = false) String state,
                                         @RequestParam(value = "error", required = false) String error,
                                         @RequestHeader(value = "Accept-Language", required = false) String lang) {
        Provider provider = Provider.fromSlug(slug).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        String binding = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (service.bindingCookieName().equals(c.getName())) {
                    binding = c.getValue();
                }
            }
        }
        return redirect(service.callback(provider, code, state, error, binding, lang));
    }

    @PostMapping("/exchange")
    public ResponseEntity<Map<String, Object>> exchange(HttpServletRequest request,
                                                        @RequestBody ExchangeRequest body) {
        ClientContext client = clients.authenticate(request, body.clientId(), true);
        TokenResponses.Result r = service.exchange(client, body.code(), body.codeVerifier(), body.redirectUri());
        ResponseEntity.BodyBuilder b = ResponseEntity.ok().cacheControl(CacheControl.noStore());
        if (r.setCookie() != null) {
            b.header(HttpHeaders.SET_COOKIE, r.setCookie());
        }
        return b.body(r.body());
    }

    /** Erros que nunca redirecionam: página HTML do Identity (social-login.md §3). */
    @ExceptionHandler(SocialPageException.class)
    ResponseEntity<String> page(SocialPageException ex, @RequestHeader(value = "Accept-Language", required = false) String lang) {
        boolean en = LocaleSupport.EN.equals(LocaleSupport.resolve(lang, LocaleSupport.PT_BR));
        String title = en ? "We could not complete your request" : "Não foi possível concluir sua solicitação";
        String body = en ? "The sign-in link is invalid or has expired. Close this window and try again from the application."
                : "O link de acesso é inválido ou expirou. Feche esta janela e tente novamente a partir do aplicativo.";
        String html = "<!doctype html><html lang=\"" + (en ? "en" : "pt-BR") + "\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>" + title
                + "</title><style>body{font-family:system-ui,sans-serif;margin:0;display:grid;place-items:center;"
                + "min-height:100vh;background:#f6f7f9;color:#1c1b1f}main{max-width:28rem;padding:2rem;background:#fff;"
                + "border-radius:12px;box-shadow:0 1px 4px rgba(0,0,0,.12)}h1{font-size:1.25rem}</style></head><body><main><h1>"
                + title + "</h1><p>" + body + "</p></main></body></html>";
        return ResponseEntity.status(HttpStatus.valueOf(ex.status())).contentType(MediaType.TEXT_HTML)
                .cacheControl(CacheControl.noStore()).header("Referrer-Policy", "no-referrer")
                .body(new String(html.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
    }

    private static ResponseEntity<Void> redirect(SocialLoginService.Redirect r) {
        ResponseEntity.BodyBuilder b = ResponseEntity.status(HttpStatus.FOUND).location(URI.create(r.location()))
                .cacheControl(CacheControl.noStore()).header("Referrer-Policy", "no-referrer");
        if (r.setCookie() != null) {
            b.header(HttpHeaders.SET_COOKIE, r.setCookie());
        }
        return b.build();
    }
}
