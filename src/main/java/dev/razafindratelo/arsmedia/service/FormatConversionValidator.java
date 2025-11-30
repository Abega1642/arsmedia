package dev.razafindratelo.arsmedia.service;

import dev.razafindratelo.arsmedia.model.classifier.ContainerFormat;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class FormatConversionValidator implements Validator {

  private static final Set<ContainerFormat> VIDEO_FORMATS =
      EnumSet.of(
          ContainerFormat.MP4,
          ContainerFormat.MKV,
          ContainerFormat.MOV,
          ContainerFormat.AVI,
          ContainerFormat.FLV,
          ContainerFormat.WMV,
          ContainerFormat.WEBM,
          ContainerFormat.MPEG_TS,
          ContainerFormat.MPEG_PS,
          ContainerFormat.THREEGP,
          ContainerFormat.ASF);

  private static final Set<ContainerFormat> AUDIO_FORMATS =
      EnumSet.of(
          ContainerFormat.MP3,
          ContainerFormat.OGG,
          ContainerFormat.M4A,
          ContainerFormat.WAV,
          ContainerFormat.FLAC,
          ContainerFormat.APE,
          ContainerFormat.AIFF);

  @Override
  public boolean supports(@Nullable Class<?> clazz) {
    return FormatConversionRequest.class.equals(clazz);
  }

  @Override
  public void validate(@Nullable Object target, @Nullable Errors errors) {
    if (target == null || errors == null) return;

    var request = (FormatConversionRequest) target;
    var source = request.sourceFormat();
    var targetF = request.targetFormat();

    rejectIf(
        targetF == ContainerFormat.UNKNOWN,
        errors,
        "format.unknown",
        "Target format is unknown or unsupported");

    rejectIf(
        source == targetF,
        errors,
        "format.same",
        "Source and target formats are the same: %s".formatted(source));

    rejectIf(
        isVideo(source) && isAudio(targetF),
        errors,
        "format.videoToAudio",
        "Cannot convert video %s to audio %s. Use audio extraction instead."
            .formatted(source, targetF));

    rejectIf(
        isAudio(source) && isVideo(targetF),
        errors,
        "format.audioToVideo",
        "Cannot convert audio %s to video %s".formatted(source, targetF));
  }

  private void rejectIf(boolean condition, Errors errors, String code, String message) {
    if (condition) {
      errors.rejectValue("targetFormat", code, message);
    }
  }

  private boolean isVideo(ContainerFormat f) {
    return VIDEO_FORMATS.contains(f);
  }

  private boolean isAudio(ContainerFormat f) {
    return AUDIO_FORMATS.contains(f);
  }

  public record FormatConversionRequest(
      ContainerFormat sourceFormat, ContainerFormat targetFormat) {}
}
