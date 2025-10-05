package dev.razafindratelo.arsmedia.mail;

import dev.razafindratelo.arsmedia.InfraGenerated;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.util.List;

@InfraGenerated
public record Email(
    InternetAddress to,
    List<InternetAddress> cc,
    List<InternetAddress> bcc,
    String subject,
    String htmlBody,
    List<File> attachments) {}
