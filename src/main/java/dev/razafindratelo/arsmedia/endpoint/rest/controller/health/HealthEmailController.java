package dev.razafindratelo.arsmedia.endpoint.rest.controller.health;

import static org.owasp.encoder.Encode.forJava;

import dev.razafindratelo.arsmedia.InfraGenerated;
import dev.razafindratelo.arsmedia.service.health.HealthEmailService;
import jakarta.mail.internet.AddressException;
import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@InfraGenerated
@Slf4j
@RestController
@AllArgsConstructor
public class HealthEmailController {

  private final HealthEmailService healthEmailService;

  /**
   * Sends a series of test emails to verify email functionality. Sends 5 different types of test
   * emails: 1. Subject only 2. With CC 3. With BCC 4. With HTML body 5. With attachment
   *
   * @param to the recipient email address for the health check
   * @return ResponseEntity with status message
   */
  @GetMapping("/health/email")
  public ResponseEntity<String> sendHealthCheckEmails(@RequestParam String to) {
    try {
      healthEmailService.sendHealthCheckEmails(to);
      String message = String.format("All 5 test emails sent successfully to %s", forJava(to));
      return ResponseEntity.ok(message);

    } catch (AddressException e) {
      log.error("Invalid email address provided: {}", forJava(to), e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body("Invalid email address: %s".formatted(forJava(to)));

    } catch (IOException e) {
      log.error("Failed to create test attachment for health check", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Failed to create test attachment");

    } catch (Exception e) {
      log.error("Unexpected error during email health check for: {}", forJava(to), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send emails");
    }
  }
}
