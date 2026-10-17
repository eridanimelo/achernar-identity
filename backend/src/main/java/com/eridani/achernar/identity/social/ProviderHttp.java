package com.eridani.achernar.identity.social;

import com.eridani.achernar.identity.shared.config.AchernarProperties;
import com.eridani.achernar.identity.shared.json.MiniJson;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.stereotype.Component;

/**
 * HTTP de saída para provedores (SEC-40): somente URLs montadas pelo código a partir de hosts fixos,
 * sem redirecionamentos, timeout de 5 s, corpo limitado.
 */
@Component
public class ProviderHttp {

    private static final int MAX_BODY = 256 * 1024;

    private final HttpClient client;
    private final Duration timeout;

    public ProviderHttp(AchernarProperties props) {
        this.timeout = props.social().httpTimeout();
        this.client = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public Map<String, Object> postForm(String url, Map<String, String> form) {
        StringJoiner body = new StringJoiner("&");
        form.forEach((k, v) -> body.add(enc(k) + "=" + enc(v)));
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(timeout)
                .header("Content-Type", "application/x-www-form-urlencoded").header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        return json(send(req));
    }

    public Map<String, Object> getJson(String url) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(timeout).header("Accept", "application/json")
                .GET().build();
        return json(send(req));
    }

    private String send(HttpRequest req) {
        try {
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() >= 500) {
                throw ProviderException.unavailable("HTTP " + res.statusCode(), null);
            }
            if (res.statusCode() >= 400 || res.body() == null || res.body().length() > MAX_BODY) {
                throw ProviderException.invalid("HTTP " + res.statusCode());
            }
            return res.body();
        } catch (IOException e) {
            throw ProviderException.unavailable("Falha de rede", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ProviderException.unavailable("Interrompido", e);
        }
    }

    private static Map<String, Object> json(String body) {
        try {
            return MiniJson.parseObject(body);
        } catch (IllegalArgumentException e) {
            throw ProviderException.invalid("Resposta do provedor não é JSON");
        }
    }

    static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
