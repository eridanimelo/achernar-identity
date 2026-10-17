package com.eridani.achernar.identity.mail;

import com.eridani.achernar.identity.shared.config.AchernarProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Renderiza assunto, texto e HTML a partir de {@code templates/mail/} (sem texto fixo no código Java).
 * Cada modelo declara chaves {@code <id>.subject}, {@code <id>.body}, {@code <id>.footer} e botões
 * {@code <id>.button}/{@code <id>.button2} associados às variáveis {@code action_url}/{@code action_url2}.
 * Variantes (ex.: desativada/reativada) usam o sufixo {@code .<variant>}.
 */
@Component
public class MailRenderer {

    public record Rendered(String subject, String text, String html, String lang) {
    }

    private final AchernarProperties props;
    private final Map<String, Properties> bundles = new ConcurrentHashMap<>();
    private final Map<String, String> layouts = new ConcurrentHashMap<>();

    public MailRenderer(AchernarProperties props) {
        this.props = props;
    }

    public Rendered render(MailTemplate template, String locale, Map<String, String> vars) {
        String lang = "en".equalsIgnoreCase(locale) || (locale != null && locale.toLowerCase().startsWith("en"))
                ? "en" : "pt-BR";
        Properties b = bundle(lang);
        Map<String, String> v = new java.util.HashMap<>(vars);
        v.putIfAbsent("support_contact", props.mail().supportContact());
        v.putIfAbsent("app_name", "Achernar Identity");
        String variant = v.get("variant");
        String id = template.id();

        String subject = fill(pick(b, id, "subject", variant), v, false);
        if (subject.length() > 70) {
            subject = subject.substring(0, 69) + "…";
        }
        String body = fill(pick(b, id, "body", variant), v, false);
        String bodyHtml = fill(pick(b, id, "body", variant), v, true);
        String footer = fill(b.getProperty("common.footer"), v, false) ;
        String footerHtml = fill(b.getProperty("common.footer"), v, true);
        String ignore = pickOptional(b, id, "ignore", variant);
        String ignoreText = ignore == null ? "" : fill(ignore, v, false);
        String ignoreHtml = ignore == null ? "" : fill(ignore, v, true);

        List<String[]> buttons = new ArrayList<>();
        addButton(buttons, b, id, variant, "button", "action_url", v);
        addButton(buttons, b, id, variant, "button2", "action_url2", v);

        StringBuilder text = new StringBuilder();
        text.append(body).append("\n\n");
        StringBuilder htmlButtons = new StringBuilder();
        for (String[] btn : buttons) {
            text.append(btn[0]).append(": ").append(btn[1]).append("\n");
            htmlButtons.append("<p style=\"margin:24px 0\"><a href=\"").append(HtmlUtils.htmlEscape(btn[1]))
                    .append("\" style=\"background:#1a4fb4;color:#ffffff;padding:12px 24px;border-radius:6px;")
                    .append("text-decoration:none;font-weight:bold;display:inline-block\">")
                    .append(HtmlUtils.htmlEscape(btn[0])).append("</a></p>\n")
                    .append("<p style=\"font-size:14px;word-break:break-all\">")
                    .append(HtmlUtils.htmlEscape(btn[1])).append("</p>\n");
        }
        if (!ignoreText.isEmpty()) {
            text.append("\n").append(ignoreText).append("\n");
        }
        text.append("\n-- \n").append(footer).append("\n");

        String html = layout()
                .replace("{{lang}}", lang)
                .replace("{{title}}", HtmlUtils.htmlEscape(subject))
                .replace("{{app_name}}", HtmlUtils.htmlEscape(v.get("app_name")))
                .replace("{{body}}", bodyHtml)
                .replace("{{buttons}}", htmlButtons.toString())
                .replace("{{ignore}}", ignoreHtml)
                .replace("{{footer}}", footerHtml);
        return new Rendered(subject, text.toString(), html, lang);
    }

    private void addButton(List<String[]> out, Properties b, String id, String variant, String part,
                           String urlVar, Map<String, String> v) {
        String label = pickOptional(b, id, part, variant);
        String url = v.get(urlVar);
        if (label != null && url != null && !url.isBlank()) {
            out.add(new String[] {fill(label, v, false), url});
        }
    }

    private static String pick(Properties b, String id, String part, String variant) {
        String s = pickOptional(b, id, part, variant);
        if (s == null) {
            throw new IllegalStateException("Chave de e-mail ausente: " + id + "." + part);
        }
        return s;
    }

    private static String pickOptional(Properties b, String id, String part, String variant) {
        if (variant != null) {
            String s = b.getProperty(id + "." + part + "." + variant);
            if (s != null) {
                return s;
            }
        }
        return b.getProperty(id + "." + part);
    }

    static String fill(String template, Map<String, String> vars, boolean html) {
        String out = template;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            String val = e.getValue() == null ? "" : e.getValue();
            out = out.replace("{{" + e.getKey() + "}}", html ? HtmlUtils.htmlEscape(val) : val);
        }
        return out;
    }

    private Properties bundle(String lang) {
        return bundles.computeIfAbsent(lang, l -> {
            Properties p = new Properties();
            try (InputStream in = getClass().getResourceAsStream("/templates/mail/mail_" + l + ".properties")) {
                if (in == null) {
                    throw new IllegalStateException("Pacote de e-mail ausente: " + l);
                }
                p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return p;
        });
    }

    private String layout() {
        return layouts.computeIfAbsent("html", k -> {
            try (InputStream in = getClass().getResourceAsStream("/templates/mail/layout.html")) {
                if (in == null) {
                    throw new IllegalStateException("Layout de e-mail ausente");
                }
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }
}
