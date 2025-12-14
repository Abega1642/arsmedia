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

  private static final int MAX_EVENTS_PER_BATCH = 10;
  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;
  private final String exchangeName;
  private final String routingKey;
  private final ListGrouper<T> listGrouper;

  public EventProducer(
      RabbitTemplate rabbitTemplate,
      ObjectMapper objectMapper,
      @Value("${spring.rabbitmq.exchange}") String exchangeName,
      @Value("${spring.rabbitmq.routing-key}") String routingKey,
      ListGrouper<T> listGrouper) {
    this.rabbitTemplate = rabbitTemplate;
    this.objectMapper = objectMapper;
    this.exchangeName = exchangeName;
    this.routingKey = routingKey;
    this.listGrouper = listGrouper;
  }

  @Override
  public void accept(Collection<T> events) {
    if (events == null || events.isEmpty()) {
      log.warn("No events to publish.");
      return;
    }

    listGrouper.apply(List.copyOf(events), MAX_EVENTS_PER_BATCH).forEach(this::publishBatch);
  }

  private void publishBatch(List<T> batch) {
    log.info(
        "Publishing batch of {} events to exchange '{}' with routing '{}'",
        batch.size(),
        exchangeName,
        routingKey);

    batch.forEach(this::publishEvent);
  }

  private void publishEvent(T event) {
    try {
      String payload = serializeEvent(event);
      sendToRabbitMQ(payload);
      logSuccessfulPublish(event);
    } catch (JsonProcessingException e) {
      logSerializationError(event, e);
    } catch (Exception e) {
      logPublishingError(event, e);
    }
  }

  private String serializeEvent(T event) throws JsonProcessingException {
    return objectMapper.writeValueAsString(event);
  }

  private void sendToRabbitMQ(String payload) {
    rabbitTemplate.convertAndSend(exchangeName, routingKey, payload);
  }

  private void logSuccessfulPublish(T event) {
    log.debug("Published event: {}", event.getClass().getSimpleName());
  }

  private void logSerializationError(T event, JsonProcessingException e) {
    log.error("Serialization failed for event: {}", event, e);
  }

  private void logPublishingError(T event, Exception e) {
    log.error("Publishing failed for event: {}", event, e);
  }
}
