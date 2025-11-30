package dev.razafindratelo.arsmedia.mapper;

import static dev.razafindratelo.arsmedia.mapper.MapperConstants.UTILITY_CLASS_EXCEPTION_MESSAGE;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job.VideoFormatConversionJobStatusResponse;
import dev.razafindratelo.arsmedia.repository.model.job.VideoFormatConversionJob;

public class VideoFormatConversionJobStatusResponseMapper {
  private VideoFormatConversionJobStatusResponseMapper() {
    throw new UnsupportedOperationException(UTILITY_CLASS_EXCEPTION_MESSAGE);
  }

  public static VideoFormatConversionJobStatusResponse mapToFormatConversionResponse(
      VideoFormatConversionJob job) {
    return VideoFormatConversionJobStatusResponse.builder()
        .jobId(job.getId())
        .status(job.getStatus())
        .createdAt(job.getCreatedAt())
        .completedAt(job.getCompletedAt())
        .convertedVideoId(job.getConvertedVideo() != null ? job.getConvertedVideo().getId() : null)
        .convertedVideoBucketKey(
            job.getConvertedVideo() != null ? job.getConvertedVideo().getBucketKey() : null)
        .errorMessage(job.getErrorMessage())
        .attemptCount(job.getAttemptCount())
        .build();
  }
}
