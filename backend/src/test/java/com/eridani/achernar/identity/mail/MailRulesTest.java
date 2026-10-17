package com.eridani.achernar.identity.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class MailRulesTest {

    @Test
    void backoffProgressivoComTeto() {
        assertThat(MailBackoff.nextDelay(1, 8).get()).isEqualTo(Duration.ofMinutes(1));
        assertThat(MailBackoff.nextDelay(2, 8).get()).isEqualTo(Duration.ofMinutes(5));
        assertThat(MailBackoff.nextDelay(3, 8).get()).isEqualTo(Duration.ofMinutes(15));
        assertThat(MailBackoff.nextDelay(4, 8).get()).isEqualTo(Duration.ofHours(1));
        assertThat(MailBackoff.nextDelay(5, 8).get()).isEqualTo(Duration.ofHours(6));
        assertThat(MailBackoff.nextDelay(7, 8).get()).isEqualTo(Duration.ofHours(6));
    }

    @Test
    void backoffEsgotadoOuInvalidoNaoReagenda() {
        assertThat(MailBackoff.nextDelay(8, 8).isEmpty()).isTrue();
        assertThat(MailBackoff.nextDelay(9, 8).isEmpty()).isTrue();
        assertThat(MailBackoff.nextDelay(0, 8).isEmpty()).isTrue();
    }

    static class SMTPAddressFailedException extends RuntimeException {
        SMTPAddressFailedException(String m) { super(m); }
    }

    static class SMTPSendFailedException extends RuntimeException {
        SMTPSendFailedException(String m) { super(m); }
    }

    static class SMTPTransientException extends RuntimeException {
        SMTPTransientException(String m) { super(m); }
    }

    @Test
    void classificaFalhaPermanente() {
        assertThat(MailFailureClassifier.isPermanent(new SMTPAddressFailedException("550 5.1.1 user unknown"))).isTrue();
        assertThat(MailFailureClassifier.isPermanent(new RuntimeException("x", new SMTPSendFailedException("550 rejected"))))
                .isTrue();
    }

    @Test
    void classificaFalhaTransiente() {
        assertThat(MailFailureClassifier.isPermanent(new SMTPTransientException("421 try later"))).isFalse();
        assertThat(MailFailureClassifier.isPermanent(new java.net.ConnectException("refused"))).isFalse();
        assertThat(MailFailureClassifier.isPermanent(new SMTPSendFailedException("451 4.3.0 temporary"))).isFalse();
    }

    @Test
    void erroSeguroNaoVazaMensagemComEnderecoOuToken() {
        String s = MailFailureClassifier.safeError(
                new SMTPAddressFailedException("550 5.1.1 <maria@exemplo.com> user unknown token=abc"));
        assertThat(s).isEqualTo("SMTPAddressFailedException 550");
        assertThat(s).doesNotContain("maria");
        assertThat(MailFailureClassifier.safeError(new IllegalStateException("sem codigo"))).isEqualTo("IllegalStateException");
    }
}
