package dev.razafindratelo.arsmedia.mapper;

import static dev.razafindratelo.arsmedia.mapper.MapperConstants.UTILITY_CLASS_EXCEPTION_MESSAGE;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job.AudioExtractionJobStatusResponse;
import dev.razafindratelo.arsmedia.repository.model.job.AudioExtractionJob;

public class AudioExtractionJobStatusResponseMapper {

  private AudioExtractionJobStatusResponseMapper() {
    throw new UnsupportedOperationException(UTILITY_CLASS_EXCEPTION_MESSAGE);
  }

  public static AudioExtractionJobStatusResponse mapToAudioExtractionJobStatusResponse(
      AudioExtractionJob job) {
    return AudioExtractionJobStatusResponse.builder()
        .jobId(job.getId())
        .status(job.getStatus())
        .createdAt(job.getCreatedAt())
        .completedAt(job.getCompletedAt())
        .extractedAudioId(job.getExtractedAudio() != null ? job.getExtractedAudio().getId() : null)
        .extractedAudioBucketKey(
            job.getExtractedAudio() != null ? job.getExtractedAudio().getBucketKey() : null)
        .errorMessage(job.getErrorMessage())
        .attemptCount(job.getAttemptCount())
        .build();
  }
}
