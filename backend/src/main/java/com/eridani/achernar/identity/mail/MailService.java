package com.eridani.achernar.identity.mail;

import com.eridani.achernar.identity.shared.config.AchernarProperties;
import com.eridani.achernar.identity.shared.util.EmailAddress;
import com.eridani.achernar.identity.shared.web.IpMasker;
import com.eridani.achernar.identity.shared.web.RequestInfoHolder;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Fachada para os fluxos de negócio: monta variáveis e enfileira na transação corrente. */
@Service
public class MailService {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'")
            .withZone(ZoneOffset.UTC);

    private final MailOutbox outbox;
    private final AchernarProperties props;

    public MailService(MailOutbox outbox, AchernarProperties props) {
        this.outbox = outbox;
        this.props = props;
    }

    /** Variáveis comuns. */
    public static Map<String, String> vars(String appName) {
        Map<String, String> v = new HashMap<>();
        if (appName != null && !appName.isBlank()) {
            v.put("app_name", appName);
        }
        return v;
    }

    public void send(MailTemplate t, String locale, String to, UUID accountId, UUID applicationId,
                     Map<String, String> vars) {
        outbox.enqueue(t, locale, to, accountId, applicationId, vars);
    }

    /** Aviso de segurança sem link (ou com {@code action_url} opcional). */
    public void notice(MailTemplate t, String locale, String to, UUID accountId, Instant at, Map<String, String> extra) {
        Map<String, String> v = new HashMap<>(extra);
        v.put("occurred_at", WHEN.format(at));
        String ip = IpMasker.approximate(RequestInfoHolder.get().ip());
        v.put("ip_approx", ip == null ? "—" : ip);
        outbox.enqueue(t, locale, to, accountId, null, v);
    }

    public static String humanDuration(Duration d, String locale) {
        boolean en = locale != null && locale.toLowerCase().startsWith("en");
        long minutes = d.toMinutes();
        if (minutes >= 60 && minutes % 60 == 0) {
            long h = minutes / 60;
            return en ? h + (h == 1 ? " hour" : " hours") : h + (h == 1 ? " hora" : " horas");
        }
        if (minutes >= 1) {
            return en ? minutes + (minutes == 1 ? " minute" : " minutes")
                    : minutes + (minutes == 1 ? " minuto" : " minutos");
        }
        long s = Math.max(1, d.toSeconds());
        return en ? s + " seconds" : s + " segundos";
    }

    public static String safeAddress(String email) {
        return EmailAddress.mask(email);
    }
}
