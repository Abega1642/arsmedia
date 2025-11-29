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
    return new VideoFormatConversionJobStatusResponse(
        job.getId(),
        job.getStatus(),
        job.getCreatedAt(),
        job.getCompletedAt(),
        job.getConvertedVideo() != null ? job.getConvertedVideo().getId() : null,
        job.getConvertedVideo() != null ? job.getConvertedVideo().getBucketKey() : null,
        job.getErrorMessage(),
        job.getAttemptCount());
  }
}
