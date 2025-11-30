package dev.razafindratelo.arsmedia.service.util;

import java.util.Map;
import java.util.function.BiFunction;
import org.springframework.stereotype.Component;

@Component
public class Pagination implements BiFunction<Integer, Integer, Map<String, Integer>> {

  @Override
  public Map<String, Integer> apply(Integer page, Integer size) {
    var fPage = (page == null) ? 0 : page;
    var fSize = (size == null) ? 10 : size;

    if (fPage < 0) throw new IllegalArgumentException("Page cannot be negative");
    if (fSize < 1) throw new IllegalArgumentException("Size cannot be less than 1.");

    return Map.of(
        "page", fPage,
        "size", fSize);
  }
}
