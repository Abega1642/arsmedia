package dev.razafindratelo.arsmedia.event.model;

import dev.razafindratelo.arsmedia.InfraGenerated;
import dev.razafindratelo.arsmedia.datastructure.ListGrouper;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@InfraGenerated
@Configuration
public class EventConf {

  @Value("${infra.rabbitmq.username}")
  private String username;

  @Value("${infra.rabbitmq.password}")
  private String password;

  @Value("${infra.rabbitmq.host}")
  private String host;

  @Value("${infra.rabbitmq.port}")
  private int port;

  @Value("${infra.rabbitmq.vhost}")
  private String vhost;

  @Bean
  public CachingConnectionFactory connectionFactory()
      throws NoSuchAlgorithmException, KeyManagementException {
    CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
    factory.setUsername(username);
    factory.setPassword(password);
    factory.setVirtualHost(vhost);

    factory.getRabbitConnectionFactory().useSslProtocol();

    factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
    factory.setPublisherReturns(true);
    return factory;
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMandatory(true);
    return template;
  }

  @Bean
  public ListGrouper<?> listGrouper() {
    return new ListGrouper<>();
  }
}
