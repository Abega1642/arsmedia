package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job.VideoCompressionJobStatusResponse;
import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.model.classifier.AudioCodec;
import dev.razafindratelo.arsmedia.model.classifier.ContainerFormat;
import dev.razafindratelo.arsmedia.model.classifier.FileType;
import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import dev.razafindratelo.arsmedia.model.classifier.SizeType;
import dev.razafindratelo.arsmedia.model.classifier.VideoCodec;
import dev.razafindratelo.arsmedia.service.CompressionVideoService;
import dev.razafindratelo.arsmedia.service.VideoService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@AutoConfigureMockMvc(addFilters = false)
@Slf4j
class VideoControllerIT extends FacadeIT {

  private static final String BASE_URL = "/api/media/videos";
  private static final String USER_EMAIL = "test@example.com";
  private static final String BUCKET_KEY = "test_video_bucket_key_123";

  @Autowired private MockMvc mvc;
  @MockitoBean private VideoService videoService;
  @MockitoBean private CompressionVideoService compressionService;

  private ResultActions postCompress(String email, String bucketKey) throws Exception {
    MockHttpServletRequestBuilder request =
        post(BASE_URL + "/compress/{userEmail}", email).contentType(MediaType.APPLICATION_JSON);
    if (bucketKey != null) request.param("bucket_key", bucketKey);
    return mvc.perform(request);
  }

  private ResultActions getCompressedVideos(String email) throws Exception {
    MockHttpServletRequestBuilder request =
        get(BASE_URL + "/compressed").contentType(MediaType.APPLICATION_JSON);
    if (email != null) request.param("userEmail", email);
    return mvc.perform(request);
  }

  private ResultActions getCompressionStatus(String jobId) throws Exception {
    return mvc.perform(
        get(BASE_URL + "/compression-status/{jobId}", jobId)
            .contentType(MediaType.APPLICATION_JSON));
  }

  private void logResponse(ResultActions result, String message) throws Exception {
    log.info("{} Response: {}", message, result.andReturn().getResponse().getContentAsString());
  }

  private VideoCompressionJobStatusResponse buildJobStatus(
      String jobId,
      ProcessStatus status,
      LocalDateTime createdAt,
      LocalDateTime completedAt,
      String compressedVideoId,
      String compressedVideoUrl,
      String errorMessage,
      int attemptCount) {
    return new VideoCompressionJobStatusResponse(
        jobId,
        status,
        createdAt,
        completedAt,
        compressedVideoId,
        compressedVideoUrl,
        errorMessage,
        attemptCount);
  }

  private List<Video> createMockCompressedVideos() {
    List<Video> videos = new ArrayList<>();
    for (int i = 1; i <= 3; i++) {
      Video v = new Video();
      v.setId(UUID.randomUUID().toString());
      v.setFileName("compressed_video_" + i + ".mp4");
      v.setFilePath("s3://bucket/compressed_" + i + ".mp4");
      v.setWidth(1280);
      v.setHeight(720);
      v.setDuration(120.0);
      v.setFrameRate(30.0);
      v.setSize(5_000_000L);
      v.setSizeType(SizeType.BYTES);
      v.setFileType(FileType.VIDEO);
      v.setCodec(VideoCodec.H264);
      v.setContainerFormat(ContainerFormat.MP4);
      v.setAudioChannels(2);
      v.setAudioSampleRate(48000);
      v.setAudioCodec(AudioCodec.AAC);
      v.setCreatedAt(LocalDateTime.now());
      videos.add(v);
    }
    return videos;
  }

  @Test
  void should_compress_video_and_return_job_status() throws Exception {
    var jobId = UUID.randomUUID().toString();
    var expected =
        buildJobStatus(
            jobId, ProcessStatus.PENDING, LocalDateTime.now(), null, null, null, null, 0);
    when(videoService.compress(USER_EMAIL, BUCKET_KEY)).thenReturn(expected);

    var result =
        postCompress(USER_EMAIL, BUCKET_KEY)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.job_id").value(jobId))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.created_at").exists())
            .andExpect(jsonPath("$.completed_at").doesNotExist())
            .andExpect(jsonPath("$.compressed_video_id").doesNotExist())
            .andExpect(jsonPath("$.compressed_video_url").doesNotExist())
            .andExpect(jsonPath("$.error_message").doesNotExist())
            .andExpect(jsonPath("$.attempt_count").value(0));

    logResponse(result, "Compression request successful");
    verify(videoService).compress(USER_EMAIL, BUCKET_KEY);
  }

  @Test
  void should_return_bad_request_when_email_is_invalid() throws Exception {
    var invalidEmail = "invalid-email";
    var result = postCompress(invalidEmail, BUCKET_KEY).andExpect(status().isBadRequest());
    logResponse(result, "Validation failed for invalid email");
    verify(videoService, never()).compress(anyString(), anyString());
  }

  @Test
  void should_return_bad_request_when_bucket_key_is_missing() throws Exception {
    var result = postCompress(USER_EMAIL, null).andExpect(status().isBadRequest());
    logResponse(result, "Validation failed for missing bucket_key");
    verify(videoService, never()).compress(anyString(), anyString());
  }

  @Test
  void should_return_not_found_when_video_does_not_exist() throws Exception {
    var nonExistentBucketKey = "non_existent_key";
    when(videoService.compress(USER_EMAIL, nonExistentBucketKey))
        .thenThrow(
            new EntityNotFoundException(
                "No video instance found with bucket_key = " + nonExistentBucketKey));

    var result = postCompress(USER_EMAIL, nonExistentBucketKey).andExpect(status().isNotFound());
    logResponse(result, "Entity not found as expected");
    verify(videoService).compress(USER_EMAIL, nonExistentBucketKey);
  }

  @Test
  void should_return_bad_request_when_email_is_blank() throws Exception {
    var blankEmail = "   ";
    var result = postCompress(blankEmail, BUCKET_KEY).andExpect(status().isBadRequest());
    logResponse(result, "Validation failed for blank email");
    verify(videoService, never()).compress(anyString(), anyString());
  }

  @Test
  void should_return_bad_request_when_bucket_key_is_blank() throws Exception {
    var blankBucketKey = "   ";
    var result = postCompress(USER_EMAIL, blankBucketKey).andExpect(status().isBadRequest());
    logResponse(result, "Validation failed for blank bucket_key");
    verify(videoService, never()).compress(anyString(), anyString());
  }

  @Test
  void should_handle_service_exception_gracefully() throws Exception {
    when(videoService.compress(USER_EMAIL, BUCKET_KEY))
        .thenThrow(new RuntimeException("Unexpected service error"));

    var result = postCompress(USER_EMAIL, BUCKET_KEY).andExpect(status().is5xxServerError());
    logResponse(result, "Service exception handled");
    verify(videoService).compress(USER_EMAIL, BUCKET_KEY);
  }

  @Test
  void should_compress_video_with_special_characters_in_email() throws Exception {
    var specialEmail = "test+user@example.com";
    var jobId = UUID.randomUUID().toString();
    var expected =
        buildJobStatus(
            jobId, ProcessStatus.PENDING, LocalDateTime.now(), null, null, null, null, 0);
    when(videoService.compress(specialEmail, BUCKET_KEY)).thenReturn(expected);

    var result =
        postCompress(specialEmail, BUCKET_KEY)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.job_id").value(jobId))
            .andExpect(jsonPath("$.status").value("PENDING"));

    logResponse(result, "Compression request for email with special chars successful");
    verify(videoService).compress(specialEmail, BUCKET_KEY);
  }

  @Test
  void should_compress_video_with_special_characters_in_bucket_key() throws Exception {
    var specialBucketKey = "videos/user_123/video-name_2024.mp4";
    var jobId = UUID.randomUUID().toString();
    var expected =
        buildJobStatus(
            jobId, ProcessStatus.PENDING, LocalDateTime.now(), null, null, null, null, 0);
    when(videoService.compress(USER_EMAIL, specialBucketKey)).thenReturn(expected);

    var result =
        postCompress(USER_EMAIL, specialBucketKey)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.job_id").value(jobId))
            .andExpect(jsonPath("$.status").value("PENDING"));

    logResponse(result, "Compression request for bucket_key with special chars successful");
    verify(videoService).compress(USER_EMAIL, specialBucketKey);
  }

  @Test
  void should_get_all_compressed_videos_for_user() throws Exception {
    List<Video> videos = createMockCompressedVideos();
    when(compressionService.getAllCompressedVideos(USER_EMAIL)).thenReturn(videos);

    var result =
        getCompressedVideos(USER_EMAIL)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(videos.size()));

    logResponse(result, "Retrieved all compressed videos");
    verify(compressionService).getAllCompressedVideos(USER_EMAIL);
  }

  @Test
  void should_return_empty_list_when_user_has_no_compressed_videos() throws Exception {
    when(compressionService.getAllCompressedVideos(USER_EMAIL)).thenReturn(List.of());

    var result =
        getCompressedVideos(USER_EMAIL)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));

    logResponse(result, "No compressed videos found for user");
    verify(compressionService).getAllCompressedVideos(USER_EMAIL);
  }

  @Test
  void should_return_bad_request_when_user_email_is_invalid_for_get_compressed() throws Exception {
    var invalidEmail = "not-an-email";
    var result = getCompressedVideos(invalidEmail).andExpect(status().isBadRequest());
    logResponse(result, "Validation failed for invalid email in GET");
    verify(compressionService, never()).getAllCompressedVideos(anyString());
  }

  @Test
  void should_return_bad_request_when_user_email_missing_get_compressed() throws Exception {
    var result = getCompressedVideos(null).andExpect(status().isBadRequest());
    logResponse(result, "Validation failed for missing userEmail");
    verify(compressionService, never()).getAllCompressedVideos(anyString());
  }

  @Test
  void should_get_compression_job_status_by_job_id() throws Exception {
    var jobId = UUID.randomUUID().toString();
    var expected =
        buildJobStatus(
            jobId,
            ProcessStatus.COMPLETED,
            LocalDateTime.now().minusMinutes(5),
            LocalDateTime.now(),
            UUID.randomUUID().toString(),
            "s3://bucket/compressed_video.mp4",
            null,
            1);
    when(videoService.getCompressionStatus(jobId)).thenReturn(expected);

    var result =
        getCompressionStatus(jobId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.job_id").value(jobId))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(
                jsonPath("$.compressed_video_url").value("s3://bucket/compressed_video.mp4"));

    logResponse(result, "Retrieved compression status");
    verify(videoService).getCompressionStatus(jobId);
  }

  @Test
  void should_return_not_found_when_job_id_does_not_exist() throws Exception {
    var jobId = UUID.randomUUID().toString();
    when(videoService.getCompressionStatus(jobId))
        .thenThrow(new EntityNotFoundException("Compression job not found: " + jobId));

    var result = getCompressionStatus(jobId).andExpect(status().isNotFound());
    logResponse(result, "Job not found as expected");
    verify(videoService).getCompressionStatus(jobId);
  }

  @Test
  void should_get_compression_job_with_progressing_status() throws Exception {
    var jobId = UUID.randomUUID().toString();
    var expected =
        buildJobStatus(
            jobId,
            ProcessStatus.PROGRESSING,
            LocalDateTime.now().minusMinutes(2),
            null,
            null,
            null,
            null,
            1);
    when(videoService.getCompressionStatus(jobId)).thenReturn(expected);

    var result =
        getCompressionStatus(jobId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.job_id").value(jobId))
            .andExpect(jsonPath("$.status").value("PROGRESSING"))
            .andExpect(jsonPath("$.completed_at").doesNotExist())
            .andExpect(jsonPath("$.compressed_video_id").doesNotExist())
            .andExpect(jsonPath("$.attempt_count").value(1));

    logResponse(result, "Job still progressing");
    verify(videoService).getCompressionStatus(jobId);
  }

  @Test
  void should_return_bad_request_when_user_email_is_missing_for_get_compressed() throws Exception {
    var result = getCompressedVideos(null).andExpect(status().isBadRequest());
    logResponse(result, "Validation failed as expected for missing userEmail");
    verify(compressionService, never()).getAllCompressedVideos(anyString());
  }

  @Test
  void should_get_compression_job_with_failed_status() throws Exception {
    var jobId = UUID.randomUUID().toString();
    var expected =
        buildJobStatus(
            jobId,
            ProcessStatus.FAILED,
            LocalDateTime.now().minusMinutes(10),
            LocalDateTime.now().minusMinutes(5),
            null,
            null,
            "FFmpeg processing failed on attempt 3",
            3);
    when(videoService.getCompressionStatus(jobId)).thenReturn(expected);

    var result =
        getCompressionStatus(jobId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.job_id").value(jobId))
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.error_message").value("FFmpeg processing failed on attempt 3"))
            .andExpect(jsonPath("$.attempt_count").value(3));

    logResponse(result, "Job failed as expected");
    verify(videoService).getCompressionStatus(jobId);
  }

  @Test
  void should_get_all_compression_jobs_for_video() throws Exception {
    var videoId = UUID.randomUUID().toString();
    var jobs =
        List.of(
            buildJobStatus(
                UUID.randomUUID().toString(),
                ProcessStatus.COMPLETED,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                UUID.randomUUID().toString(),
                "s3://bucket/compressed1.mp4",
                null,
                1),
            buildJobStatus(
                UUID.randomUUID().toString(),
                ProcessStatus.FAILED,
                LocalDateTime.now().minusMinutes(30),
                LocalDateTime.now().minusMinutes(25),
                null,
                null,
                "Upload failed",
                2),
            buildJobStatus(
                UUID.randomUUID().toString(),
                ProcessStatus.PROGRESSING,
                LocalDateTime.now().minusMinutes(5),
                null,
                null,
                null,
                null,
                1));
    when(videoService.getCompressionJobsByVideoId(videoId)).thenReturn(jobs);

    var result =
        mvc.perform(
                get(BASE_URL + "/compression-jobs/{videoId}", videoId)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(3));

    logResponse(result, "Retrieved all compression jobs for video");
    verify(videoService).getCompressionJobsByVideoId(videoId);
  }
}
