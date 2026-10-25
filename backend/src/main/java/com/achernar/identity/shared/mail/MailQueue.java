package com.achernar.identity.shared.mail;

import java.util.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class MailQueue {
    private final NamedParameterJdbcTemplate db;

    public MailQueue(NamedParameterJdbcTemplate db) {
        this.db = db;
    }

    public void enqueue(String email, String subject, String body) {
        db.update("INSERT INTO mail_outbox(id,recipient,subject,body) VALUES(:id,:email,:subject,:body)",
                Map.of("id", UUID.randomUUID(), "email", email, "subject", subject, "body", body));
    }
}
