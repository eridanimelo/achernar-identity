package com.achernar.identity.shared.mail;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mail.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MailDelivery {
    private record Pending(UUID id, String recipient, String subject, String body) {
    }

    private final NamedParameterJdbcTemplate db;
    private final MailSender sender;
    private final String from;

    public MailDelivery(NamedParameterJdbcTemplate db, MailSender sender, @Value("${app.mail-from}") String from) {
        this.db = db;
        this.sender = sender;
        this.from = from;
    }

    @Scheduled(fixedDelayString = "${app.mail-delay:5000}", initialDelayString = "${app.mail-initial-delay:1000}")
    @Transactional
    public void deliver() {
        var pending = db.query(
                "SELECT id,recipient,subject,body FROM mail_outbox WHERE sent_at IS NULL AND next_attempt<=CURRENT_TIMESTAMP ORDER BY next_attempt LIMIT 10 FOR UPDATE SKIP LOCKED",
                Map.of(), (r, n) -> new Pending(r.getObject("id", UUID.class), r.getString("recipient"),
                        r.getString("subject"), r.getString("body")));
        for (var item : pending) {
            var message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(item.recipient());
            message.setSubject(item.subject());
            message.setText(item.body());
            try {
                sender.send(message);
                db.update("UPDATE mail_outbox SET sent_at=CURRENT_TIMESTAMP,body='',recipient='' WHERE id=:id",
                        Map.of("id", item.id()));
            } catch (MailException e) {
                db.update(
                        "UPDATE mail_outbox SET attempts=attempts+1,next_attempt=CURRENT_TIMESTAMP+INTERVAL '1 minute'*LEAST(attempts+1,60) WHERE id=:id",
                        Map.of("id", item.id()));
                org.slf4j.LoggerFactory.getLogger(MailDelivery.class).warn("Entrega adiada para mensagem {}",
                        item.id());
            }
        }
    }
}
