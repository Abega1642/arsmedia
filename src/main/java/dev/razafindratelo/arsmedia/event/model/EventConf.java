package dev.razafindratelo.arsmedia.event.model;

import dev.razafindratelo.arsmedia.InfraGenerated;
import dev.razafindratelo.arsmedia.datastructure.ListGrouper;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@InfraGenerated
@Configuration
public class EventConf {

  @Value("${spring.rabbitmq.username}")
  private String username;

  @Value("${spring.rabbitmq.password}")
  private String password;

  @Value("${spring.rabbitmq.host}")
  private String host;

  @Value("${spring.rabbitmq.port}")
  private int port;

  @Value("${spring.rabbitmq.vhost:/}")
  private String vhost;

  @Value("${app.rabbitmq.ssl:false}")
  private boolean sslEnabled;

  @Bean
  public CachingConnectionFactory connectionFactory() {
    CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
    factory.setUsername(username);
    factory.setPassword(password);
    factory.setVirtualHost((vhost == null || vhost.isBlank()) ? "/" : vhost);

    if (sslEnabled) {
      try {
        factory.getRabbitConnectionFactory().useSslProtocol();
      } catch (Exception e) {
        throw new IllegalStateException("Failed to enable SSL for RabbitMQ", e);
      }
    }

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
  public ListGrouper<InfraEvent> listGrouper() {
    return new ListGrouper<>();
  }
}
