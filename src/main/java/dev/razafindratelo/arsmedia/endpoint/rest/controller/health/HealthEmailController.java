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

  private final Mailer mailer;

  @GetMapping("/health/email")
  public ResponseEntity<String> sendEmails(@RequestParam String to) {
    try {
      log.info("Starting email health check for: {}", to);

      var toAddress = new InternetAddress(to);
      toAddress.validate();

      String emailUser = to.split("@")[0];
      String emailDomain = "@" + to.split("@")[1];

      mailer.accept(
          new Email(
              toAddress,
              List.of(),
              List.of(),
              "[arsmedia health check 1/5] Subject only",
              null,
              List.of()));

      mailer.accept(
          new Email(
              toAddress,
              List.of(new InternetAddress(emailUser + "+cc" + emailDomain)),
              List.of(),
              "[arsmedia health check 2/5] With cc",
              null,
              List.of()));

      mailer.accept(
          new Email(
              toAddress,
              List.of(),
              List.of(new InternetAddress(emailUser + "+bcc" + emailDomain)),
              "[arsmedia health check 3/5] With bcc",
              null,
              List.of()));

      mailer.accept(
          new Email(
              toAddress,
              List.of(),
              List.of(),
              "[arsmedia health check 4/5] With body",
              "<div><h1>Hello from Arsmedia!</h1><p>This is a <b>test email</b> with HTML"
                  + " content.</p></div>",
              List.of()));

      mailer.accept(
          new Email(
              toAddress,
              List.of(),
              List.of(),
              "[arsmedia health check 5/5] With attachment",
              "<p>This email has an attachment</p>",
              List.of(createTempFile())));

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

  private File createTempFile() throws IOException {
    File tempFile = File.createTempFile("test-attachment", ".txt");
    Files.writeString(
        tempFile.toPath(),
        "This is a test attachment from Arsmedia.\nTimestamp: " + System.currentTimeMillis());
    tempFile.deleteOnExit();
    return tempFile;
  }
}
