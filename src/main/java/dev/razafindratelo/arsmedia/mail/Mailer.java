package dev.razafindratelo.arsmedia.mail;

import dev.razafindratelo.arsmedia.InfraGenerated;
import dev.razafindratelo.arsmedia.config.EmailConf;
import dev.razafindratelo.arsmedia.exception.EmailSendException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
    if (!isValidEmail(email)) {
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
      MimeMessage message = createMimeMessage(email);
      mailSender.send(message);
      log.info("Email sent successfully to {}", email.to().getAddress());
    } catch (MessagingException e) {
      log.error(
          "MessagingException while sending email to {}: {}",
          email.to().getAddress(),
          e.getMessage(),
          e);
      throw new EmailSendException("Failed to send email", e);
    } catch (Exception e) {
      log.error(
          "Unexpected error while sending email to {}: {}",
          email.to().getAddress(),
          e.getMessage(),
          e);
      throw new EmailSendException("Unexpected error sending email", e);
    }
  }

  private MimeMessage createMimeMessage(Email email) throws MessagingException {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

    configureBasicEmailProperties(helper, email);
    configureCarbonCopyRecipients(helper, email);
    configureEmailBody(helper, email);
    attachFiles(helper, email);

    return message;
  }

  private void configureBasicEmailProperties(MimeMessageHelper helper, Email email)
      throws MessagingException {
    helper.setFrom(emailConf.getFromEmail());
    helper.setTo(email.to().getAddress());
    helper.setSubject(email.subject());
  }

  private void configureCarbonCopyRecipients(MimeMessageHelper helper, Email email)
      throws MessagingException {
    if (hasRecipients(email.cc())) {
      helper.setCc(toAddressArray(email.cc()));
    }

    if (hasRecipients(email.bcc())) {
      helper.setBcc(toAddressArray(email.bcc()));
    }
  }

  private void configureEmailBody(MimeMessageHelper helper, Email email) throws MessagingException {
    if (hasContent(email.htmlBody())) {
      helper.setText(email.htmlBody(), true);
    } else {
      helper.setText("(no content — Arsmedia health check)", false);
    }
  }

  private void attachFiles(MimeMessageHelper helper, Email email) {
    if (email.attachments() == null) {
      return;
    }

    for (File file : email.attachments()) {
      addAttachmentSafely(helper, file);
    }
  }

  private void addAttachmentSafely(MimeMessageHelper helper, File file) {
    try {
      helper.addAttachment(file.getName(), file);
    } catch (Exception ex) {
      log.warn("Failed to attach file {}: {}", file.getName(), ex.getMessage(), ex);
    }
  }

  private boolean isValidEmail(Email email) {
    return email != null && email.to() != null;
  }

  private boolean hasRecipients(List<InternetAddress> recipients) {
    return recipients != null && !recipients.isEmpty();
  }

  private boolean hasContent(String content) {
    return content != null && !content.isEmpty();
  }

  private String[] toAddressArray(List<InternetAddress> addresses) {
    return addresses.stream().map(InternetAddress::getAddress).toArray(String[]::new);
  }
}
