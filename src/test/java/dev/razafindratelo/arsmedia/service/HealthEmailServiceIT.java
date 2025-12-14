package dev.razafindratelo.arsmedia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.razafindratelo.arsmedia.InfraGenerated;
import dev.razafindratelo.arsmedia.mail.Email;
import dev.razafindratelo.arsmedia.mail.Mailer;
import jakarta.mail.internet.AddressException;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@InfraGenerated
class HealthEmailServiceIT {

  private static final String VALID_EMAIL = "test@example.com";
  private static final String INVALID_EMAIL = "invalid-email";
  private static final String HEALTH_CHECK_PREFIX = "[arsmedia health check";
  @TempDir Path tempDir;
  @Mock private Mailer mailer;
  @InjectMocks private HealthEmailService healthEmailService;
  @Captor private ArgumentCaptor<Email> emailCaptor;

  @Test
  void should_send_all_five_health_check_emails_successfully() throws Exception {
    healthEmailService.sendHealthCheckEmails(VALID_EMAIL);

    verify(mailer, times(5)).accept(any(Email.class));
  }

  @Test
  void should_send_subject_only_email_as_first_test() throws Exception {
    healthEmailService.sendHealthCheckEmails(VALID_EMAIL);

    verify(mailer, times(5)).accept(emailCaptor.capture());
    List<Email> sentEmails = emailCaptor.getAllValues();

    Email firstEmail = sentEmails.getFirst();
    assertEquals(VALID_EMAIL, firstEmail.to().getAddress());
    assertTrue(firstEmail.subject().contains("1/5] Subject only"));
    assertTrue(firstEmail.cc().isEmpty());
    assertTrue(firstEmail.bcc().isEmpty());
    assertNull(firstEmail.htmlBody());
    assertTrue(firstEmail.attachments().isEmpty());
  }

  @Test
  void should_send_email_with_cc_as_second_test() throws Exception {
    healthEmailService.sendHealthCheckEmails(VALID_EMAIL);

    verify(mailer, times(5)).accept(emailCaptor.capture());
    List<Email> sentEmails = emailCaptor.getAllValues();

    Email secondEmail = sentEmails.get(1);
    assertEquals(VALID_EMAIL, secondEmail.to().getAddress());
    assertTrue(secondEmail.subject().contains("2/5] With cc"));
    assertEquals(1, secondEmail.cc().size());
    assertEquals("test+cc@example.com", secondEmail.cc().getFirst().getAddress());
    assertTrue(secondEmail.bcc().isEmpty());
  }

  @Test
  void should_send_email_with_bcc_as_third_test() throws Exception {
    healthEmailService.sendHealthCheckEmails(VALID_EMAIL);

    verify(mailer, times(5)).accept(emailCaptor.capture());
    List<Email> sentEmails = emailCaptor.getAllValues();

    Email thirdEmail = sentEmails.get(2);
    assertEquals(VALID_EMAIL, thirdEmail.to().getAddress());
    assertTrue(thirdEmail.subject().contains("3/5] With bcc"));
    assertTrue(thirdEmail.cc().isEmpty());
    assertEquals(1, thirdEmail.bcc().size());
    assertEquals("test+bcc@example.com", thirdEmail.bcc().getFirst().getAddress());
  }

  @Test
  void should_send_email_with_html_body_as_fourth_test() throws Exception {
    healthEmailService.sendHealthCheckEmails(VALID_EMAIL);

    verify(mailer, times(5)).accept(emailCaptor.capture());
    List<Email> sentEmails = emailCaptor.getAllValues();

    Email fourthEmail = sentEmails.get(3);
    assertEquals(VALID_EMAIL, fourthEmail.to().getAddress());
    assertTrue(fourthEmail.subject().contains("4/5] With body"));
    assertNotNull(fourthEmail.htmlBody());
    assertTrue(fourthEmail.htmlBody().contains("Hello from Arsmedia!"));
    assertTrue(fourthEmail.htmlBody().contains("<h1>"));
  }

  @Test
  void should_send_email_with_attachment_as_fifth_test() throws Exception {
    healthEmailService.sendHealthCheckEmails(VALID_EMAIL);

    verify(mailer, times(5)).accept(emailCaptor.capture());
    List<Email> sentEmails = emailCaptor.getAllValues();

    Email fifthEmail = sentEmails.get(4);
    assertEquals(VALID_EMAIL, fifthEmail.to().getAddress());
    assertTrue(fifthEmail.subject().contains("5/5] With attachment"));
    assertNotNull(fifthEmail.htmlBody());
    assertEquals(1, fifthEmail.attachments().size());

    File attachment = fifthEmail.attachments().getFirst();
    assertTrue(attachment.getName().startsWith("test-attachment"));
    assertTrue(attachment.getName().endsWith(".txt"));
  }

  @Test
  void should_throw_address_exception_when_email_is_invalid() {
    assertThrows(
        AddressException.class,
        () -> {
          healthEmailService.sendHealthCheckEmails(INVALID_EMAIL);
        });

    verify(mailer, never()).accept(any(Email.class));
  }

  @Test
  void should_throw_illegal_argument_exception_when_email_has_no_at_symbol() {
    assertThrows(
        AddressException.class,
        () -> {
          healthEmailService.sendHealthCheckEmails("emailwithoutatsymbol");
        });

    verify(mailer, never()).accept(any(Email.class));
  }

  @Test
  void should_handle_email_with_subdomain_correctly() throws Exception {
    String emailWithSubdomain = "user@mail.example.com";

    healthEmailService.sendHealthCheckEmails(emailWithSubdomain);

    verify(mailer, times(5)).accept(emailCaptor.capture());
    List<Email> sentEmails = emailCaptor.getAllValues();

    Email emailWithCc = sentEmails.get(1);
    assertEquals("user+cc@mail.example.com", emailWithCc.cc().getFirst().getAddress());
  }

  @Test
  void should_handle_email_with_dots_in_local_part() throws Exception {
    String emailWithDots = "first.last@example.com";

    healthEmailService.sendHealthCheckEmails(emailWithDots);

    verify(mailer, times(5)).accept(emailCaptor.capture());
    List<Email> sentEmails = emailCaptor.getAllValues();

    Email emailWithCc = sentEmails.get(1);
    assertEquals("first.last+cc@example.com", emailWithCc.cc().getFirst().getAddress());
  }

  @Test
  void should_send_emails_in_correct_order() throws Exception {
    healthEmailService.sendHealthCheckEmails(VALID_EMAIL);

    verify(mailer, times(5)).accept(emailCaptor.capture());
    List<Email> sentEmails = emailCaptor.getAllValues();

    assertTrue(sentEmails.get(0).subject().contains("1/5"));
    assertTrue(sentEmails.get(1).subject().contains("2/5"));
    assertTrue(sentEmails.get(2).subject().contains("3/5"));
    assertTrue(sentEmails.get(3).subject().contains("4/5"));
    assertTrue(sentEmails.get(4).subject().contains("5/5"));
  }

  @Test
  void should_use_correct_health_check_prefix_in_all_subjects() throws Exception {
    healthEmailService.sendHealthCheckEmails(VALID_EMAIL);

    verify(mailer, times(5)).accept(emailCaptor.capture());
    List<Email> sentEmails = emailCaptor.getAllValues();

    sentEmails.forEach(email -> assertTrue(email.subject().startsWith(HEALTH_CHECK_PREFIX)));
  }
}
