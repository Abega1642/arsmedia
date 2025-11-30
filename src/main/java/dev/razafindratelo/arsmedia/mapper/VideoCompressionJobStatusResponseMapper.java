package dev.razafindratelo.arsmedia.mapper;

import static dev.razafindratelo.arsmedia.mapper.MapperConstants.UTILITY_CLASS_EXCEPTION_MESSAGE;
import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toVideo;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job.VideoCompressionJobStatusResponse;
import dev.razafindratelo.arsmedia.repository.model.job.VideoCompressionJob;

public class VideoCompressionJobStatusResponseMapper {

  private VideoCompressionJobStatusResponseMapper() {
    throw new UnsupportedOperationException(UTILITY_CLASS_EXCEPTION_MESSAGE);
  }

  public static VideoCompressionJobStatusResponse mapToCompressionResponse(
      VideoCompressionJob job) {
    return VideoCompressionJobStatusResponse.builder()
        .jobId(job.getId())
        .status(job.getStatus())
        .createdAt(job.getCreatedAt())
        .completedAt(job.getCompletedAt())
        .compressedVideoId(
            job.getCompressedVideo() != null ? job.getCompressedVideo().getId() : null)
        .compressedVideoUrl(
            job.getCompressedVideo() != null
                ? toVideo(job.getCompressedVideo()).getFilePath()
                : null)
        .errorMessage(job.getErrorMessage())
        .attemptCount(job.getAttemptCount())
        .build();
  }
}
