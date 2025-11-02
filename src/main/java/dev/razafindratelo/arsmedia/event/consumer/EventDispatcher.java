package dev.razafindratelo.arsmedia.event.consumer;

import dev.razafindratelo.arsmedia.InfraGenerated;
import dev.razafindratelo.arsmedia.event.model.InfraEvent;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@InfraGenerated
@Component
@Slf4j
public class EventDispatcher implements Consumer<InfraEvent>, ApplicationContextAware {

  private ApplicationContext applicationContext;

  @Override
  public void setApplicationContext(@NonNull ApplicationContext applicationContext)
      throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public void accept(InfraEvent event) {
    String eventSimpleName = event.getClass().getSimpleName();
    String serviceBeanName =
        Character.toLowerCase(eventSimpleName.charAt(0)) + eventSimpleName.substring(1) + "Service";

    try {
      @SuppressWarnings("unchecked")
      Consumer<InfraEvent> consumer =
          (Consumer<InfraEvent>) applicationContext.getBean(serviceBeanName);

      log.info("Dispatching {} to {}", eventSimpleName, serviceBeanName);
      consumer.accept(event);

    } catch (NoSuchBeanDefinitionException e) {
      log.warn(
          "No service found for event {} (looking for bean: {})", eventSimpleName, serviceBeanName);
    }
  }
}
