package dev.razafindratelo.arsmedia.config;

import dev.razafindratelo.arsmedia.InfraGenerated;
import java.util.Properties;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@InfraGenerated
@Getter
@Configuration
public class EmailConf {
  private static final String TRUE = "true";
  private static final String MAIL_TRANSPORT_PROTOCOL = "mail.transport.protocol";
  private static final String SMTP_AUTH = "mail.smtp.auth";
  private static final String SMTP_STARTTLS = "mail.smtp.starttls.enable";
  private static final String MAIL_DEBUG = "mail.debug";

  private final String smtpHost;
  private final int smtpPort;
  private final String username;
  private final String password;
  private final String fromEmail;

  public EmailConf(
      @Value("${spring.mail.host}") String smtpHost,
      @Value("${spring.mail.port}") int smtpPort,
      @Value("${spring.mail.username}") String username,
      @Value("${spring.mail.password}") String password,
      @Value("${spring.mail.from-email}") String fromEmail) {
    this.smtpHost = smtpHost;
    this.smtpPort = smtpPort;
    this.username = username;
    this.password = password;
    this.fromEmail = fromEmail;
  }

  @Bean
  public JavaMailSender mailSender() {
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost(smtpHost);
    mailSender.setPort(smtpPort);
    mailSender.setUsername(username);
    mailSender.setPassword(password);

    Properties props = mailSender.getJavaMailProperties();
    props.put(MAIL_TRANSPORT_PROTOCOL, "smtp");
    props.put(SMTP_AUTH, TRUE);
    props.put(SMTP_STARTTLS, TRUE);
    props.put(MAIL_DEBUG, TRUE);

    return mailSender;
  }
}
