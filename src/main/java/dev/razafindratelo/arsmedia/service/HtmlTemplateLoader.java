package dev.razafindratelo.arsmedia.service;

import dev.razafindratelo.arsmedia.exception.TemplateLoadingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class HtmlTemplateLoader implements BiFunction<String, Map<String, String>, String> {

  @Override
  public String apply(String fileName, Map<String, String> variables) {
    var template = load(fileName);
    return injectVariables(template, variables);
  }

  public String load(String fileName) {
    try (var inputStream = new ClassPathResource("static/" + fileName).getInputStream()) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new TemplateLoadingException("Failed to load HTML template: " + fileName, e);
    }
  }

  /** inject variables to placeholders of type {{PLACEHOLDER}} */
  private String injectVariables(String template, Map<String, String> variables) {
    if (variables == null || variables.isEmpty()) return template;

    Pattern pattern = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");
    Matcher matcher = pattern.matcher(template);
    StringBuilder sb = new StringBuilder();

    while (matcher.find()) {
      String key = matcher.group(1);
      String value = variables.getOrDefault(key, "");
      matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
    }
    matcher.appendTail(sb);

    return sb.toString();
  }
}
