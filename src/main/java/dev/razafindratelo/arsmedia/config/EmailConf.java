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
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.debug", "true");

    return mailSender;
  }
}
