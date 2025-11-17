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

  private final String transportProtocol;
  private final String smtpAuth;
  private final String smtpStarttls;
  private final String mailDebug;

  public EmailConf(
      @Value("${spring.mail.host}") String smtpHost,
      @Value("${spring.mail.port}") int smtpPort,
      @Value("${spring.mail.username}") String username,
      @Value("${spring.mail.password}") String password,
      @Value("${spring.mail.from-email}") String fromEmail,
      @Value("${mail.transport.protocol:smtp}") String transportProtocol,
      @Value("${mail.smtp.auth:true}") String smtpAuth,
      @Value("${mail.smtp.starttls.enable:true}") String smtpStarttls,
      @Value("${mail.debug:false}") String mailDebug) {
    this.smtpHost = smtpHost;
    this.smtpPort = smtpPort;
    this.username = username;
    this.password = password;
    this.fromEmail = fromEmail;
    this.transportProtocol = transportProtocol;
    this.smtpAuth = smtpAuth;
    this.smtpStarttls = smtpStarttls;
    this.mailDebug = mailDebug;
  }

  @Bean
  public JavaMailSender mailSender() {
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost(smtpHost);
    mailSender.setPort(smtpPort);
    mailSender.setUsername(username);
    mailSender.setPassword(password);

    Properties props = mailSender.getJavaMailProperties();
    props.put("mail.transport.protocol", transportProtocol);
    props.put("mail.smtp.auth", smtpAuth);
    props.put("mail.smtp.starttls.enable", smtpStarttls);
    props.put("mail.debug", mailDebug);

    return mailSender;
  }
}
