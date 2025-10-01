package dev.razafindratelo.arsmedia.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

  @Value("${infra.rabbitmq.queue}")
  private String queueName;

  @Value("${infra.rabbitmq.exchange}")
  private String exchangeName;

  @Value("${infra.rabbitmq.routing-key}")
  private String routingKey;

  @Bean
  public Queue myQueue() {
    return new Queue(queueName, true);
  }

  @Bean
  public DirectExchange myExchange() {
    return new DirectExchange(exchangeName);
  }

  @Bean
  public Binding binding(Queue myQueue, DirectExchange myExchange) {
    return BindingBuilder.bind(myQueue).to(myExchange).with(routingKey);
  }
}
