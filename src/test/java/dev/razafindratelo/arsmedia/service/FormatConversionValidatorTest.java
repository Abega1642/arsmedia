package dev.razafindratelo.arsmedia.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.razafindratelo.arsmedia.model.classifier.ContainerFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

class FormatConversionValidatorTest {
  private FormatConversionValidator validator;

  @BeforeEach
  void setUp() {
    validator = new FormatConversionValidator();
  }

  private Errors validate(FormatConversionValidator.FormatConversionRequest req) {
    Errors errors = new BeanPropertyBindingResult(req, "request");
    validator.validate(req, errors);
    return errors;
  }

  private FormatConversionValidator.FormatConversionRequest req(
      ContainerFormat src, ContainerFormat tgt) {
    return new FormatConversionValidator.FormatConversionRequest(src, tgt);
  }

  private void assertErrorCode(Errors errors, String expectedCode) {
    assertThat(errors.getFieldError("targetFormat"))
        .isNotNull()
        .extracting(DefaultMessageSourceResolvable::getCode)
        .isEqualTo(expectedCode);
  }

  @Test
  @DisplayName("should_accept_valid_format_conversion")
  void should_accept_valid_format_conversion() {
    Errors errors = validate(req(ContainerFormat.MP4, ContainerFormat.MKV));

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  @DisplayName("should_reject_unknown_target_format")
  void should_reject_unknown_target_format() {
    Errors errors = validate(req(ContainerFormat.MP4, ContainerFormat.UNKNOWN));

    assertThat(errors.hasErrors()).isTrue();
    assertErrorCode(errors, "format.unknown");
  }

  @Test
  @DisplayName("should_reject_same_source_and_target_format")
  void should_reject_same_source_and_target_format() {
    Errors errors = validate(req(ContainerFormat.MP3, ContainerFormat.MP3));

    assertThat(errors.hasErrors()).isTrue();
    assertErrorCode(errors, "format.same");
  }

  @Test
  @DisplayName("should_reject_video_to_audio_conversion")
  void should_reject_video_to_audio_conversion() {
    Errors errors = validate(req(ContainerFormat.MP4, ContainerFormat.MP3));

    assertThat(errors.hasErrors()).isTrue();
    assertErrorCode(errors, "format.videoToAudio");
  }

  @Test
  @DisplayName("should_reject_audio_to_video_conversion")
  void should_reject_audio_to_video_conversion() {
    Errors errors = validate(req(ContainerFormat.MP3, ContainerFormat.MP4));

    assertThat(errors.hasErrors()).isTrue();
    assertErrorCode(errors, "format.audioToVideo");
  }

  @Test
  @DisplayName("supports_should_only_accept_FormatConversionRequest")
  void supports_should_only_accept_FormatConversionRequest() {
    assertThat(validator.supports(FormatConversionValidator.FormatConversionRequest.class))
        .isTrue();
    assertThat(validator.supports(String.class)).isFalse();
    assertThat(validator.supports(null)).isFalse();
  }
}
