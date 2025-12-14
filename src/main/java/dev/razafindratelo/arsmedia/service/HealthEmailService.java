package dev.razafindratelo.arsmedia.service;

import static org.owasp.encoder.Encode.forJava;

import dev.razafindratelo.arsmedia.InfraGenerated;
import dev.razafindratelo.arsmedia.mail.Email;
import dev.razafindratelo.arsmedia.mail.Mailer;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for handling email health checks. Sends a series of test emails to verify email
 * functionality.
 */
@Slf4j
@Service
@AllArgsConstructor
@InfraGenerated
public class HealthEmailService {

  private static final String HEALTH_CHECK_PREFIX = "[arsmedia health check";
  private static final String TEST_ATTACHMENT_FILENAME = "test-attachment";
  private static final String TEST_ATTACHMENT_EXTENSION = ".txt";

  private final Mailer mailer;

  /**
   * Sends a comprehensive set of test emails to verify email functionality.
   *
   * @param recipientEmail the email address to send test emails to
   * @throws AddressException if the email address is invalid
   * @throws IOException if there's an error creating the test attachment
   */
  public void sendHealthCheckEmails(String recipientEmail) throws AddressException, IOException {
    log.info("Starting email health check for: {}", forJava(recipientEmail));

    InternetAddress toAddress = validateAndParseEmail(recipientEmail);
    EmailComponents emailComponents = parseEmailComponents(recipientEmail);

    List<EmailTestCase> testCases = buildTestCases(toAddress, emailComponents);
    executeTestCases(testCases);

    log.info("Email health check completed successfully for: {}", forJava(recipientEmail));
  }

  private List<EmailTestCase> buildTestCases(
      InternetAddress toAddress, EmailComponents components) {
    return List.of(
        new EmailTestCase("subject-only", () -> sendSubjectOnlyEmail(toAddress)),
        new EmailTestCase("with-cc", () -> sendEmailWithCc(toAddress, components)),
        new EmailTestCase("with-bcc", () -> sendEmailWithBcc(toAddress, components)),
        new EmailTestCase("with-body", () -> sendEmailWithBody(toAddress)),
        new EmailTestCase("with-attachment", () -> sendEmailWithAttachment(toAddress)));
  }

  private void executeTestCases(List<EmailTestCase> testCases) throws IOException {
    for (EmailTestCase testCase : testCases) {
      try {
        testCase.execute();
      } catch (IOException e) {
        log.error("Failed to execute test case: {}", testCase.name(), e);
        throw e;
      } catch (Exception e) {
        log.error("Unexpected error in test case: {}", testCase.name(), e);
        throw new IOException("Email health check failed: " + testCase.name(), e);
      }
    }
  }

  private InternetAddress validateAndParseEmail(String email) throws AddressException {
    InternetAddress address = new InternetAddress(email);
    address.validate();
    return address;
  }

  private EmailComponents parseEmailComponents(String email) {
    int lastAtIndex = email.lastIndexOf('@');
    if (lastAtIndex <= 0 || lastAtIndex == email.length() - 1) {
      throw new IllegalArgumentException("Invalid email format: " + forJava(email));
    }

    String localPart = email.substring(0, lastAtIndex);
    String domain = "@" + email.substring(lastAtIndex + 1);

    return new EmailComponents(localPart, domain);
  }

  private void sendSubjectOnlyEmail(InternetAddress toAddress) {
    mailer.accept(createEmail(toAddress, null, null, "1/5] Subject only", null, List.of()));
    log.debug("Sent subject-only test email");
  }

  private void sendEmailWithCc(InternetAddress toAddress, EmailComponents components)
      throws AddressException {
    InternetAddress ccAddress =
        new InternetAddress(components.localPart() + "+cc" + components.domain());
    mailer.accept(
        createEmail(toAddress, List.of(ccAddress), null, "2/5] With cc", null, List.of()));
    log.debug("Sent test email with CC");
  }

  private void sendEmailWithBcc(InternetAddress toAddress, EmailComponents components)
      throws AddressException {
    InternetAddress bccAddress =
        new InternetAddress(components.localPart() + "+bcc" + components.domain());
    mailer.accept(
        createEmail(toAddress, null, List.of(bccAddress), "3/5] With bcc", null, List.of()));
    log.debug("Sent test email with BCC");
  }

  private void sendEmailWithBody(InternetAddress toAddress) {
    String htmlBody =
        """
        <div>
            <h1>Hello from Arsmedia!</h1>
            <p>This is a <b>test email</b> with HTML content.</p>
        </div>
        """;
    mailer.accept(createEmail(toAddress, null, null, "4/5] With body", htmlBody, List.of()));
    log.debug("Sent test email with HTML body");
  }

  private void sendEmailWithAttachment(InternetAddress toAddress) throws IOException {
    File attachment = createSecureTempFile();
    try {
      mailer.accept(
          createEmail(
              toAddress,
              null,
              null,
              "5/5] With attachment",
              "<p>This email has an attachment</p>",
              List.of(attachment)));
      log.debug("Sent test email with attachment");
    } finally {
      cleanupTempFile(attachment);
    }
  }

  private Email createEmail(
      InternetAddress to,
      List<InternetAddress> cc,
      List<InternetAddress> bcc,
      String subjectSuffix,
      String body,
      List<File> attachments) {
    return new Email(
        to,
        cc != null ? cc : List.of(),
        bcc != null ? bcc : List.of(),
        HEALTH_CHECK_PREFIX + " " + subjectSuffix,
        body,
        attachments);
  }

  private File createSecureTempFile() throws IOException {
    try {
      return createTempFileWithPosixPermissions();
    } catch (UnsupportedOperationException e) {
      log.warn("POSIX permissions not supported, falling back to legacy method");
      return createTempFileWithLegacyPermissions();
    }
  }

  private File createTempFileWithPosixPermissions() throws IOException {
    Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
    Path tempPath =
        Files.createTempFile(
            TEST_ATTACHMENT_FILENAME,
            TEST_ATTACHMENT_EXTENSION,
            PosixFilePermissions.asFileAttribute(perms));

    writeAttachmentContent(tempPath);

    File tempFile = tempPath.toFile();
    tempFile.deleteOnExit();

    log.debug("Created secure temporary file: {}", tempPath);
    return tempFile;
  }

  private File createTempFileWithLegacyPermissions() throws IOException {
    File tempFile = File.createTempFile(TEST_ATTACHMENT_FILENAME, TEST_ATTACHMENT_EXTENSION);

    setSecureFilePermissions(tempFile);
    writeAttachmentContent(tempFile.toPath());

    tempFile.deleteOnExit();
    return tempFile;
  }

  private void setSecureFilePermissions(File file) throws IOException {
    if (!file.setReadable(false, false)) {
      throw new IOException("Failed to remove read permissions from temporary file");
    }
    if (!file.setWritable(false, false)) {
      throw new IOException("Failed to remove write permissions from temporary file");
    }
    if (!file.setExecutable(false, false)) {
      throw new IOException("Failed to remove execute permissions from temporary file");
    }
    if (!file.setReadable(true, true)) {
      throw new IOException("Failed to set owner-only read permission on temporary file");
    }
    if (!file.setWritable(true, true)) {
      throw new IOException("Failed to set owner-only write permission on temporary file");
    }
  }

  private void writeAttachmentContent(Path path) throws IOException {
    String content =
        String.format(
            "This is a test attachment from Arsmedia.%nTimestamp: %d", System.currentTimeMillis());
    Files.writeString(path, content);
  }

  private void cleanupTempFile(File file) {
    if (file != null && file.exists()) {
      try {
        Files.delete(file.toPath());
        log.debug("Cleaned up temporary file: {}", file.getAbsolutePath());
      } catch (IOException e) {
        log.warn("Failed to delete temporary file: {}", file.getAbsolutePath(), e);
      }
    }
  }

  @FunctionalInterface
  private interface TestCaseExecutor {
    void execute() throws Exception;
  }

  /** Record to hold parsed email components. */
  private record EmailComponents(String localPart, String domain) {}

  /** Record to encapsulate a test case with its name and execution logic. */
  private record EmailTestCase(String name, TestCaseExecutor executor) {
    void execute() throws Exception {
      executor.execute();
    }
  }
}
