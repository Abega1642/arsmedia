package dev.razafindratelo.arsmedia.mail;

import dev.razafindratelo.arsmedia.InfraGenerated;
import dev.razafindratelo.arsmedia.config.EmailConf;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

@InfraGenerated
@Slf4j
@Component
@RequiredArgsConstructor
public class Mailer implements Consumer<Email> {

  private final JavaMailSender mailSender;
  private final EmailConf emailConf;

  @Override
  public void accept(Email email) {
    if (email == null || email.to() == null) {
      log.warn("Email or recipient is null. Skipping send.");
      return;
    }

    try {
      send(email);
    } catch (Exception e) {
      log.error("Failed to send email to {}: {}", email.to().getAddress(), e.getMessage(), e);
    }
  }

  private void send(Email email) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper =
          new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

      helper.setFrom(emailConf.getFromEmail());
      helper.setTo(email.to().getAddress());

      if (email.cc() != null && !email.cc().isEmpty()) {
        helper.setCc(email.cc().stream().map(InternetAddress::getAddress).toArray(String[]::new));
      }

      if (email.bcc() != null && !email.bcc().isEmpty()) {
        helper.setBcc(email.bcc().stream().map(InternetAddress::getAddress).toArray(String[]::new));
      }

      helper.setSubject(email.subject());

      if (email.htmlBody() != null && !email.htmlBody().isEmpty()) {
        helper.setText(email.htmlBody(), true);
      } else {
        helper.setText("(no content — Arsmedia health check)", false);
      }

      if (email.attachments() != null) {
        for (File file : email.attachments()) {
          addAttachmentSafely(helper, file);
        }
      }

      mailSender.send(message);
      log.info("Email sent successfully to {}", email.to().getAddress());

    } catch (MessagingException e) {
      log.error(
          "MessagingException while sending email to {}: {}",
          email.to().getAddress(),
          e.getMessage(),
          e);
    } catch (Exception e) {
      log.error(
          "Unexpected error while sending email to {}: {}",
          email.to().getAddress(),
          e.getMessage(),
          e);
    }
  }

  private void addAttachmentSafely(MimeMessageHelper helper, File file) {
    try {
      helper.addAttachment(file.getName(), file);
    } catch (Exception ex) {
      log.warn("Failed to attach file {}: {}", file.getName(), ex.getMessage(), ex);
    }
  }
}
