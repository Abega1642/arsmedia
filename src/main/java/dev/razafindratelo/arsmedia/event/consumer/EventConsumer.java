package dev.razafindratelo.arsmedia.event.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.razafindratelo.arsmedia.InfraGenerated;
import dev.razafindratelo.arsmedia.event.model.InfraEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@InfraGenerated
@Component
@Slf4j
public class EventConsumer implements Consumer<String> {

  private final EventDispatcher eventHandler;
  private final ObjectMapper objectMapper;
  private final ExecutorService executor;

  public EventConsumer(EventDispatcher eventHandler, ObjectMapper objectMapper) {
    this.eventHandler = eventHandler;
    this.objectMapper = objectMapper;
    this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  log.info("Shutting down event consumer executor");
                  executor.shutdown();
                }));
  }

  @RabbitListener(queues = "${spring.rabbitmq.queue}")
  public void onMessage(String rawMessage) {
    accept(rawMessage);
  }

  @Override
  public void accept(String rawMessage) {
    executor.submit(
        () -> {
          try {
            InfraEvent event = deserialize(rawMessage);
            if (event == null) {
              log.warn("Received unprocessable event: {}", rawMessage);
              return;
            }
            eventHandler.accept(event);
            log.info("Event dispatched: {}", event.getClass().getSimpleName());
          } catch (Exception e) {
            log.error("Error while consuming event: {}", rawMessage, e);
          }
        });
  }

  private InfraEvent deserialize(String rawMessage) {
    try {
      return objectMapper.readValue(rawMessage, InfraEvent.class);
    } catch (JsonProcessingException e) {
      log.error("Deserialization failed: {}", rawMessage, e);
      return null;
    }
  }
}
