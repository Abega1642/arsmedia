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

    FormatConversionRequest request = (FormatConversionRequest) target;
    ContainerFormat sourceFormat = request.sourceFormat();
    ContainerFormat targetFormat = request.targetFormat();

    if (targetFormat == ContainerFormat.UNKNOWN)
      errors.rejectValue(
          "targetFormat", "format.unknown", "Target format is unknown or unsupported");

    if (sourceFormat == targetFormat)
      errors.rejectValue(
          "targetFormat",
          "format.same",
          String.format("Source and target formats are the same: %s", sourceFormat));

    boolean sourceIsVideo = VIDEO_FORMATS.contains(sourceFormat);
    boolean targetIsAudio = AUDIO_FORMATS.contains(targetFormat);

    if (sourceIsVideo && targetIsAudio)
      errors.rejectValue(
          "targetFormat",
          "format.videoToAudio",
          String.format(
              "Cannot convert video format %s to audio format %s. Use audio extraction instead.",
              sourceFormat, targetFormat));

    boolean sourceIsAudio = AUDIO_FORMATS.contains(sourceFormat);
    boolean targetIsVideo = VIDEO_FORMATS.contains(targetFormat);

    if (sourceIsAudio && targetIsVideo)
      errors.rejectValue(
          "targetFormat",
          "format.audioToVideo",
          String.format(
              "Cannot convert audio format %s to video format %s", sourceFormat, targetFormat));
  }

  public record FormatConversionRequest(
      ContainerFormat sourceFormat, ContainerFormat targetFormat) {}
}
