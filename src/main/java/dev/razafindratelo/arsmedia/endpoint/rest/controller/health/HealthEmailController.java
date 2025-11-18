package dev.razafindratelo.arsmedia.endpoint.rest.controller.health;

import dev.razafindratelo.arsmedia.InfraGenerated;
import dev.razafindratelo.arsmedia.mail.Email;
import dev.razafindratelo.arsmedia.mail.Mailer;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@InfraGenerated
@Slf4j
@RestController
@AllArgsConstructor
public class HealthEmailController {

  private static final String HEALTH_CHECK_PREFIX = "[arsmedia health check %d/5] ";
  private static final String EMAIL_DELIMITER = "@";

  private final Mailer mailer;

  @GetMapping("/health/email")
  public ResponseEntity<String> sendEmails(@RequestParam String to) {
    try {
      log.info("Starting email health check for: {}", to);

      InternetAddress toAddress = validateAddress(to);
      sendAllHealthCheckEmails(toAddress);

      log.info("Email health check completed successfully for: {}", to);
      return ResponseEntity.ok("All 5 test emails sent successfully to " + to);

    } catch (AddressException e) {
      log.error("Invalid email address: {}", to, e);
      return ResponseEntity.badRequest().body("Invalid email address: " + to);
    } catch (IOException e) {
      log.error("Failed to create test attachment", e);
      return ResponseEntity.internalServerError().body("Failed to create test attachment");
    } catch (Exception e) {
      log.error("Failed to send health check emails to: {}", to, e);
      return ResponseEntity.internalServerError().body("Failed to send emails: " + e.getMessage());
    }
  }

  private InternetAddress validateAddress(String email) throws AddressException {
    InternetAddress address = new InternetAddress(email);
    address.validate();
    return address;
  }

  private void sendAllHealthCheckEmails(InternetAddress toAddress)
      throws AddressException, IOException {
    String[] emailParts = toAddress.getAddress().split(EMAIL_DELIMITER, 2);
    String localPart = emailParts[0];
    String domain = EMAIL_DELIMITER + emailParts[1];

    sendEmail(toAddress, List.of(), List.of(), "Subject only", null, List.of(), 1);
    sendEmail(
        toAddress,
        createAddressList(localPart + "+cc" + domain),
        List.of(),
        "With cc",
        null,
        List.of(),
        2);
    sendEmail(
        toAddress,
        List.of(),
        createAddressList(localPart + "+bcc" + domain),
        "With bcc",
        null,
        List.of(),
        3);
    sendEmail(toAddress, List.of(), List.of(), "With body", createHtmlBody(), List.of(), 4);
    sendEmail(
        toAddress,
        List.of(),
        List.of(),
        "With attachment",
        "<p>This email has an attachment</p>",
        List.of(createTempFile()),
        5);
  }

  private void sendEmail(
      InternetAddress to,
      List<InternetAddress> cc,
      List<InternetAddress> bcc,
      String subjectSuffix,
      String body,
      List<File> attachments,
      int testNumber) {
    String subject = String.format(HEALTH_CHECK_PREFIX, testNumber) + subjectSuffix;
    mailer.accept(new Email(to, cc, bcc, subject, body, attachments));
  }

  private List<InternetAddress> createAddressList(String email) throws AddressException {
    return List.of(new InternetAddress(email));
  }

  private String createHtmlBody() {
    return "<div><h1>Hello from Arsmedia!</h1><p>This is a <b>test email</b> with HTML"
        + " content.</p></div>";
  }

  private File createTempFile() throws IOException {
    File tempFile = File.createTempFile("test-attachment", ".txt");
    String content =
        "This is a test attachment from Arsmedia.\nTimestamp: " + System.currentTimeMillis();
    Files.writeString(tempFile.toPath(), content);
    tempFile.deleteOnExit();
    return tempFile;
  }
}
