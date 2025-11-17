package dev.razafindratelo.arsmedia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.razafindratelo.arsmedia.exception.TemplateLoadingException;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class HtmlTemplateLoaderTest {
  public static final String PREFIX = "/static/";
  private final HtmlTemplateLoader subject = new HtmlTemplateLoader();

  @Test
  void should_inject_variables_correctly() throws Exception {
    var htmlFile = "test-auth-code.html";
    var resourceUrl = getClass().getResource(PREFIX + htmlFile);
    assertNotNull(resourceUrl);

    var code = "123456";
    var variables = Map.of("USERNAME", "JohnDoe", "CODE", code);

    var html = subject.apply(htmlFile, variables);
    assertTrue(html.contains("Hello JohnDoe,"));
    assertTrue(html.contains(code));
  }

  @Test
  void should_leave_unknown_placeholders_empty() throws Exception {
    String htmlFile = "unknown-placeholder.html";
    var resource = new ClassPathResource(PREFIX + htmlFile).getFile();
    try (var out = new FileOutputStream(resource)) {
      out.write("Hello {{USERNAME}}, your city is {{CITY}}.".getBytes(StandardCharsets.UTF_8));
    }
    var expected = "Hello Razafindratelo, your city is .";
    var variables = Map.of("USERNAME", "Razafindratelo");

    var html = subject.apply(htmlFile, variables);

    assertEquals(expected, html);
  }

  @Test
  void should_throw_if_template_missing() {
    assertThrows(
        TemplateLoadingException.class, () -> subject.apply("nonexistent.html", Map.of("A", "B")));
  }

  @Test
  void should_return_raw_template_when_variables_null_or_empty() throws Exception {
    var htmlFile = "test-auth-code.html";
    var resourceUrl = getClass().getResource(PREFIX + htmlFile);
    assertNotNull(resourceUrl);

    var html1 = subject.apply(htmlFile, null);
    var html2 = subject.apply(htmlFile, Map.of());

    assertTrue(html1.contains("{{USERNAME}}"));
    assertTrue(html1.contains("{{CODE}}"));
    assertTrue(html2.contains("{{USERNAME}}"));
    assertTrue(html2.contains("{{CODE}}"));
  }
}
