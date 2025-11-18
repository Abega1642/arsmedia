package dev.razafindratelo.arsmedia.mail;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;

class MailerIT extends FacadeIT {

  @TempDir Path tempDir;
  @Autowired private Mailer mailer;
  @Autowired private JavaMailSender mailSender;

  private InternetAddress testRecipient;
  private InternetAddress ccRecipient;
  private InternetAddress bccRecipient;

  private static List<InternetAddress> emptyAddressList() {
    return List.of();
  }

  private static List<File> emptyFileList() {
    return List.of();
  }

  @BeforeEach
  void setUp() throws Exception {
    testRecipient = new InternetAddress("test@example.com");
    ccRecipient = new InternetAddress("cc@example.com");
    bccRecipient = new InternetAddress("bcc@example.com");
  }

  @Test
  void should_send_simple_email() {
    var email = createEmail("Test Subject", "<p>Test Body</p>");

    assertEmailSent(email);
  }

  @Test
  void should_send_email_with_cc() {
    var email = createEmailWithCc();

    assertEmailSent(email);
  }

  @Test
  void should_send_email_with_bcc() {
    var email = createEmailWithBcc();

    assertEmailSent(email);
  }

  @Test
  void should_send_email_with_cc_and_bcc() {
    var email = createEmailWithCcAndBcc();

    assertEmailSent(email);
  }

  @Test
  void should_send_email_without_html_body() {
    var email = createEmail("Test without HTML", null);

    assertEmailSent(email);
  }

  @Test
  void should_send_email_with_empty_html_body() {
    var email = createEmail("Test with empty HTML", "");

    assertEmailSent(email);
  }

  @Test
  void should_send_email_with_attachment() throws IOException {
    var attachment = createTestFile("test-attachment.txt", "This is test content");
    var email =
        createEmailWithAttachments("Test with Attachment", "<p>See attachment</p>", attachment);

    assertEmailSent(email);
    assertTrue(attachment.exists(), "Attachment file should still exist after sending");
  }

  @Test
  void should_send_email_with_multiple_attachments() throws IOException {
    var attachment1 = createTestFile("attachment1.txt", "Content 1");
    var attachment2 = createTestFile("attachment2.txt", "Content 2");
    var attachment3 = createTestFile("attachment3.pdf", "PDF Content");

    var email =
        createEmailWithAttachments(
            "Test with Multiple Attachments",
            "<p>See attachments</p>",
            attachment1,
            attachment2,
            attachment3);

    assertEmailSent(email);
  }

  @Test
  void should_handle_null_recipient_gracefully() {
    var email =
        new Email(
            null,
            emptyAddressList(),
            emptyAddressList(),
            "Test Subject",
            "<p>Test Body</p>",
            emptyFileList());

    assertEmailSent(email);
  }

  @Test
  void should_handle_null_cc_list() {
    var email =
        new Email(
            testRecipient,
            null,
            emptyAddressList(),
            "Test Subject",
            "<p>Test Body</p>",
            emptyFileList());

    assertEmailSent(email);
  }

  @Test
  void should_handle_null_bcc_list() {
    var email =
        new Email(
            testRecipient,
            emptyAddressList(),
            null,
            "Test Subject",
            "<p>Test Body</p>",
            emptyFileList());

    assertEmailSent(email);
  }

  @Test
  void should_handle_null_attachments_list() {
    var email =
        new Email(
            testRecipient,
            emptyAddressList(),
            emptyAddressList(),
            "Test Subject",
            "<p>Test Body</p>",
            null);

    assertEmailSent(email);
  }

  @Test
  void should_handle_empty_lists() {
    var email = createEmail("Test Subject", "<p>Test Body</p>");

    assertEmailSent(email);
  }

  @Test
  void should_send_email_with_long_subject() {
    var longSubject =
        "This is a very long subject line that might cause issues if not handled properly by the"
            + " email system and we want to make sure it works correctly";
    var email = createEmail(longSubject, "<p>Test Body</p>");

    assertEmailSent(email);
  }

  @Test
  void should_send_email_with_unicode_characters() {
    var email = createEmail("Test with émojis 🎉 and ñoñó", "<p>Unicode test: こんにちは 你好 مرحبا</p>");

    assertEmailSent(email);
  }

  @Test
  void should_continue_sending_even_if_one_attachment_fails() throws IOException {
    var validAttachment = createTestFile("valid.txt", "Valid content");
    var invalidAttachment = new File("/non/existent/path/invalid.txt");

    var email =
        createEmailWithAttachments(
            "Test with Invalid Attachment",
            "<p>Mixed attachments</p>",
            validAttachment,
            invalidAttachment);

    assertEmailSent(email);
  }

  @Test
  void should_send_email_with_complex_html() {
    var complexHtml =
        """
        <html>
          <head>
            <style>
              body { font-family: Arial; }
              .header { color: blue; }
            </style>
          </head>
          <body>
            <div class="header">
              <h1>Welcome!</h1>
            </div>
            <p>This is a <strong>complex</strong> HTML email.</p>
            <ul>
              <li>Item 1</li>
              <li>Item 2</li>
            </ul>
          </body>
        </html>
        """;

    var email = createEmail("Complex HTML Test", complexHtml);

    assertEmailSent(email);
  }

  @Test
  void should_handle_empty_file() throws IOException {
    var emptyFile = createTestFile("empty.txt", "");
    var email =
        createEmailWithAttachments(
            "Test with Empty Attachment", "<p>Empty file attached</p>", emptyFile);

    assertEmailSent(email);
  }

  @Test
  void should_handle_binary_file_attachment() throws IOException {
    var binaryFile = createBinaryFile(new byte[] {0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE});
    var email =
        createEmailWithAttachments(
            "Test with Binary Attachment", "<p>Binary file attached</p>", binaryFile);

    assertEmailSent(email);
  }

  private Email createEmail(String subject, String htmlBody) {
    return new Email(
        testRecipient, emptyAddressList(), emptyAddressList(), subject, htmlBody, emptyFileList());
  }

  private Email createEmailWithCc() {
    return new Email(
        testRecipient,
        List.of(ccRecipient),
        emptyAddressList(),
        "Test with CC",
        "<p>Test Body</p>",
        emptyFileList());
  }

  private Email createEmailWithBcc() {
    return new Email(
        testRecipient,
        emptyAddressList(),
        List.of(bccRecipient),
        "Test with BCC",
        "<p>Test Body</p>",
        emptyFileList());
  }

  private Email createEmailWithCcAndBcc() {
    return new Email(
        testRecipient,
        List.of(ccRecipient),
        List.of(bccRecipient),
        "Test with CC and BCC",
        "<p>Test Body</p>",
        emptyFileList());
  }

  private Email createEmailWithAttachments(String subject, String htmlBody, File... attachments) {
    return new Email(
        testRecipient,
        emptyAddressList(),
        emptyAddressList(),
        subject,
        htmlBody,
        Arrays.asList(attachments));
  }

  private void assertEmailSent(Email email) {
    assertDoesNotThrow(
        () -> mailer.accept(email), "Email should be sent without throwing exception");
  }

  private File createTestFile(String filename, String content) throws IOException {
    var filePath = tempDir.resolve(filename);
    Files.writeString(filePath, content);
    var file = filePath.toFile();
    assertTrue(file.exists(), "Test file should be created: " + filename);
    return file;
  }

  private File createBinaryFile(byte[] content) throws IOException {
    var filePath = tempDir.resolve("binary.bin");
    Files.write(filePath, content);
    var file = filePath.toFile();
    assertTrue(file.exists(), "Binary file should be created: " + "binary.bin");
    return file;
  }
}
