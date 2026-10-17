package com.eridani.achernar.identity.mail;

import com.eridani.achernar.identity.shared.config.AchernarProperties;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/** Envio SMTP multipart (texto + HTML), UTF-8, sem rastreadores nem imagens remotas. */
@Component
public class SmtpMailSender {

    private final JavaMailSender sender;
    private final AchernarProperties props;

    public SmtpMailSender(JavaMailSender sender, AchernarProperties props) {
        this.sender = sender;
        this.props = props;
    }

    public void send(String to, MailRenderer.Rendered r) throws Exception {
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(props.mail().from(), props.mail().fromName());
        if (props.mail().replyTo() != null && !props.mail().replyTo().isBlank()) {
            helper.setReplyTo(props.mail().replyTo());
        }
        helper.setTo(to);
        helper.setSubject(r.subject());
        helper.setText(r.text(), r.html());
        message.setHeader("Content-Language", r.lang());
        message.setHeader("Auto-Submitted", "auto-generated");
        sender.send(message);
    }
}
