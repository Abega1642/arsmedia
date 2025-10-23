package dev.razafindratelo.arsmedia.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class HtmlTemplateLoaderTest {
  private final HtmlTemplateLoader subject = new HtmlTemplateLoader();

  @Test
  void should_inject_variables_correctly() throws Exception {
    var resourceUrl = getClass().getResource("/static/test-auth-code.html");
    assertNotNull(resourceUrl);

    var variables =
        Map.of(
            "USERNAME", "JohnDoe",
            "CODE", "123456");

    var html = subject.apply("test-auth-code.html", variables);
    assertTrue(html.contains("Hello JohnDoe,"));
    assertTrue(html.contains("123456"));
  }

  @Test
  void should_leave_unknown_placeholders_empty() throws Exception {
    var resource = new ClassPathResource("static/unknown-placeholder.html").getFile();
    try (var out = new FileOutputStream(resource)) {
      out.write("Hello {{USERNAME}}, your city is {{CITY}}.".getBytes(StandardCharsets.UTF_8));
    }
    var expected = "Hello Razafindratelo, your city is .";
    var variables = Map.of("USERNAME", "Razafindratelo");

    var html = subject.apply("unknown-placeholder.html", variables);

    assertEquals(expected, html);
  }

  @Test
  void should_throw_if_template_missing() {
    assertThrows(RuntimeException.class, () -> subject.apply("nonexistent.html", Map.of("A", "B")));
  }

  @Test
  void should_return_raw_template_when_variables_null_or_empty() throws Exception {
    var resourceUrl = getClass().getResource("/static/test-auth-code.html");
    assertNotNull(resourceUrl);

    var html1 = subject.apply("test-auth-code.html", null);
    var html2 = subject.apply("test-auth-code.html", Map.of());

    assertTrue(html1.contains("{{USERNAME}}"));
    assertTrue(html1.contains("{{CODE}}"));
    assertTrue(html2.contains("{{USERNAME}}"));
    assertTrue(html2.contains("{{CODE}}"));
  }
}
