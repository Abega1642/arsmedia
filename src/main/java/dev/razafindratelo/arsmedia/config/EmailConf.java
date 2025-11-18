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

  @Value("${spring.mail.host}")
  private String smtpHost;

  @Value("${spring.mail.port}")
  private int smtpPort;

  @Value("${spring.mail.username}")
  private String username;

  @Value("${spring.mail.password}")
  private String password;

  @Value("${spring.mail.from-email}")
  private String fromEmail;

  @Value("${mail.transport.protocol:smtp}")
  private String transportProtocol;

  @Value("${mail.smtp.auth:true}")
  private String smtpAuth;

  @Value("${mail.smtp.starttls.enable:true}")
  private String smtpStarttls;

  @Value("${mail.debug:false}")
  private String mailDebug;

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
