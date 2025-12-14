package dev.razafindratelo.arsmedia.endpoint.rest.controller.health;

import static java.util.UUID.randomUUID;

import dev.razafindratelo.arsmedia.InfraGenerated;
import dev.razafindratelo.arsmedia.event.model.DummyEvent;
import dev.razafindratelo.arsmedia.event.model.EventProducer;
import java.util.List;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@InfraGenerated
@RestController
@AllArgsConstructor
public class HealthEventController {
  private final EventProducer<DummyEvent> eventProducer;

  @GetMapping("/health/message")
  public List<String> triggerDummyEvents(
      @RequestParam(defaultValue = "1") int nbEvent,
      @RequestParam(defaultValue = "2") int waitInSeconds) {

    validateNbEvent(nbEvent);
    List<String> uuids = generateUuids(nbEvent);
    fireEvents(uuids, waitInSeconds);
    return uuids;
  }

  private void validateNbEvent(int nbEvent) {
    int maxEvent = 500;
    int minEvent = 1;

    if (nbEvent < minEvent || nbEvent > maxEvent)
      throw new IllegalArgumentException("nbEvent must be between 1 and 500");
  }

  private List<String> generateUuids(int nbEvent) {
    return IntStream.range(0, nbEvent).mapToObj(_ -> randomUUID().toString()).toList();
  }

  private void fireEvents(List<String> uuids, int waitInSeconds) {
    eventProducer.accept(uuids.stream().map(uuid -> new DummyEvent(uuid, waitInSeconds)).toList());
  }
}
