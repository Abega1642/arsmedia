package dev.razafindratelo.arsmedia.event.consumer;

import dev.razafindratelo.arsmedia.InfraGenerated;
import dev.razafindratelo.arsmedia.event.model.InfraEvent;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@InfraGenerated
@Component
public class EventDispatcher implements Consumer<InfraEvent> {
  @Override
  public void accept(InfraEvent infraEvent) {}
}
