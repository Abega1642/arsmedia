package dev.razafindratelo.arsmedia.event.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.razafindratelo.arsmedia.InfraGenerated;
import dev.razafindratelo.arsmedia.datastructure.ListGrouper;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@InfraGenerated
@Component
@Slf4j
public class EventProducer<T extends InfraEvent> implements Consumer<Collection<T>> {

  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;
  private final String exchangeName;
  private final String routingKey;
  private final ListGrouper<T> listGrouper;

  private static final int MAX_EVENTS_PER_BATCH = 10;

  public EventProducer(
      RabbitTemplate rabbitTemplate,
      ObjectMapper objectMapper,
      @Value("${infra.rabbitmq.exchange}") String exchangeName,
      @Value("${infra.rabbitmq.routing-key}") String routingKey,
      ListGrouper<T> listGrouper) {
    this.rabbitTemplate = rabbitTemplate;
    this.objectMapper = objectMapper;
    this.exchangeName = exchangeName;
    this.routingKey = routingKey;
    this.listGrouper = listGrouper;
  }

  @Override
  public void accept(Collection<T> events) {
    for (List<T> batch : listGrouper.apply(events.stream().toList(), MAX_EVENTS_PER_BATCH)) {
      log.info("Publishing batch of {} events", batch.size());
      for (T event : batch) {
        try {
          String payload = objectMapper.writeValueAsString(event);
          rabbitTemplate.convertAndSend(exchangeName, routingKey, payload);
          log.debug("Published event: {}", payload);
        } catch (JsonProcessingException e) {
          log.error("Serialization failed for event: {}", event, e);
        } catch (Exception e) {
          log.error("Publishing failed for event: {}", event, e);
        }
      }
    }
  }
}
