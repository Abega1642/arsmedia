package dev.razafindratelo.arsmedia.mapper;

import static dev.razafindratelo.arsmedia.mapper.MapperConstants.UTILITY_CLASS_EXCEPTION_MESSAGE;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job.AudioExtractionJobStatusResponse;
import dev.razafindratelo.arsmedia.repository.model.AudioExtractionJob;

public class AudioExtractionJobStatusResponseMapper {

  private AudioExtractionJobStatusResponseMapper() {
    throw new UnsupportedOperationException(UTILITY_CLASS_EXCEPTION_MESSAGE);
  }

  public static AudioExtractionJobStatusResponse mapToAudioExtractionJobStatusResponse(
      AudioExtractionJob job) {
    return new AudioExtractionJobStatusResponse(
        job.getId(),
        job.getStatus(),
        job.getCreatedAt(),
        job.getCompletedAt(),
        job.getExtractedAudio().getId(),
        job.getExtractedAudio().getBucketKey(),
        job.getErrorMessage(),
        job.getAttemptCount());
  }
}
