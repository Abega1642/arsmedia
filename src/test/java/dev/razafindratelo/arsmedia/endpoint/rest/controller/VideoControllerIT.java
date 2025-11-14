package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job.VideoCompressionJobStatusResponse;
import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.model.classifier.*;
import dev.razafindratelo.arsmedia.service.*;
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

@AutoConfigureMockMvc(addFilters = false)
@Slf4j
class VideoControllerIT extends FacadeIT {

  private static final String BASE_URL = "/api/media/videos";
  private static final String USER_EMAIL = "test@example.com";
  private static final String BUCKET_KEY = "test_video_bucket_key_123";
  @Autowired private MockMvc mvc;
  @MockitoBean private VideoService videoService;
  @MockitoBean private CompressionVideoService compressionService;

  @Test
  void should_compress_video_and_return_job_status() throws Exception {
    var jobId = UUID.randomUUID().toString();
    var expectedResponse =
        new VideoCompressionJobStatusResponse(
            jobId, ProcessStatus.PENDING, LocalDateTime.now(), null, null, null, null, 0);

    when(videoService.compress(USER_EMAIL, BUCKET_KEY)).thenReturn(expectedResponse);

    mvc.perform(
            post(BASE_URL + "/compress/{userEmail}", USER_EMAIL)
                .param("bucket_key", BUCKET_KEY)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.job_id").value(jobId))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.created_at").exists())
        .andExpect(jsonPath("$.completed_at").doesNotExist())
        .andExpect(jsonPath("$.compressed_video_id").doesNotExist())
        .andExpect(jsonPath("$.compressed_video_url").doesNotExist())
        .andExpect(jsonPath("$.error_message").doesNotExist())
        .andExpect(jsonPath("$.attempt_count").value(0))
        .andDo(
            result ->
                log.info(
                    "Compression request successful. Response: {}",
                    result.getResponse().getContentAsString()));

    verify(videoService).compress(USER_EMAIL, BUCKET_KEY);
  }

  @Test
  void should_return_bad_request_when_email_is_invalid() throws Exception {
    var invalidEmail = "invalid-email";

    mvc.perform(
            post(BASE_URL + "/compress/{userEmail}", invalidEmail)
                .param("bucket_key", BUCKET_KEY)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andDo(
            result ->
                log.info(
                    "Validation failed as expected for invalid email: {}",
                    result.getResponse().getContentAsString()));

    verify(videoService, never()).compress(anyString(), anyString());
  }

  @Test
  void should_return_bad_request_when_bucket_key_is_missing() throws Exception {
    mvc.perform(
            post(BASE_URL + "/compress/{userEmail}", USER_EMAIL)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andDo(
            result ->
                log.info(
                    "Validation failed as expected for missing bucket_key: {}",
                    result.getResponse().getContentAsString()));

    verify(videoService, never()).compress(anyString(), anyString());
  }

  @Test
  void should_return_not_found_when_video_does_not_exist() throws Exception {
    var nonExistentBucketKey = "non_existent_key";
    when(videoService.compress(USER_EMAIL, nonExistentBucketKey))
        .thenThrow(
            new EntityNotFoundException(
                "No video instance found with bucket_key = " + nonExistentBucketKey));

    mvc.perform(
            post(BASE_URL + "/compress/{userEmail}", USER_EMAIL)
                .param("bucket_key", nonExistentBucketKey)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andDo(
            result ->
                log.info(
                    "Entity not found as expected: {}", result.getResponse().getContentAsString()));

    verify(videoService).compress(USER_EMAIL, nonExistentBucketKey);
  }

  @Test
  void should_return_bad_request_when_email_is_blank() throws Exception {
    var blankEmail = "   ";

    mvc.perform(
            post(BASE_URL + "/compress/{userEmail}", blankEmail)
                .param("bucket_key", BUCKET_KEY)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andDo(
            result ->
                log.info(
                    "Validation failed as expected for blank email: {}",
                    result.getResponse().getContentAsString()));

    verify(videoService, never()).compress(anyString(), anyString());
  }

  @Test
  void should_return_bad_request_when_bucket_key_is_blank() throws Exception {
    var blankBucketKey = "   ";

    mvc.perform(
            post(BASE_URL + "/compress/{userEmail}", USER_EMAIL)
                .param("bucket_key", blankBucketKey)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andDo(
            result ->
                log.info(
                    "Validation failed as expected for blank bucket_key: {}",
                    result.getResponse().getContentAsString()));

    verify(videoService, never()).compress(anyString(), anyString());
  }

  @Test
  void should_get_all_compressed_videos_for_user() throws Exception {
    List<Video> mockCompressedVideos = createMockCompressedVideos();
    when(compressionService.getAllCompressedVideos(USER_EMAIL)).thenReturn(mockCompressedVideos);

    mvc.perform(
            get(BASE_URL + "/compressed")
                .param("userEmail", USER_EMAIL)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].id").exists())
        .andExpect(jsonPath("$[0].file_name").exists())
        .andExpect(jsonPath("$[0].width").exists())
        .andExpect(jsonPath("$[0].height").exists())
        .andExpect(jsonPath("$[1].id").exists())
        .andExpect(jsonPath("$[2].id").exists())
        .andDo(
            result ->
                log.info(
                    "Retrieved {} compressed videos for user {}. Response: {}",
                    mockCompressedVideos.size(),
                    USER_EMAIL,
                    result.getResponse().getContentAsString()));

    verify(compressionService).getAllCompressedVideos(USER_EMAIL);
  }

  @Test
  void should_return_empty_list_when_user_has_no_compressed_videos() throws Exception {
    when(compressionService.getAllCompressedVideos(USER_EMAIL)).thenReturn(List.of());

    mvc.perform(
            get(BASE_URL + "/compressed")
                .param("userEmail", USER_EMAIL)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0))
        .andDo(
            result ->
                log.info(
                    "No compressed videos found for user {}. Response: {}",
                    USER_EMAIL,
                    result.getResponse().getContentAsString()));

    verify(compressionService).getAllCompressedVideos(USER_EMAIL);
  }

  @Test
  void should_return_bad_request_when_user_email_is_invalid_for_get_compressed() throws Exception {
    var invalidEmail = "not-an-email";

    mvc.perform(
            get(BASE_URL + "/compressed")
                .param("userEmail", invalidEmail)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andDo(
            result ->
                log.info(
                    "Validation failed as expected for invalid email in GET: {}",
                    result.getResponse().getContentAsString()));

    verify(compressionService, never()).getAllCompressedVideos(anyString());
  }

  @Test
  void should_return_bad_request_when_user_email_is_missing_for_get_compressed() throws Exception {
    mvc.perform(get(BASE_URL + "/compressed").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andDo(
            result ->
                log.info(
                    "Validation failed as expected for missing userEmail: {}",
                    result.getResponse().getContentAsString()));

    verify(compressionService, never()).getAllCompressedVideos(anyString());
  }

  @Test
  void should_handle_service_exception_gracefully() throws Exception {
    when(videoService.compress(USER_EMAIL, BUCKET_KEY))
        .thenThrow(new RuntimeException("Unexpected service error"));

    mvc.perform(
            post(BASE_URL + "/compress/{userEmail}", USER_EMAIL)
                .param("bucket_key", BUCKET_KEY)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().is5xxServerError())
        .andDo(
            result ->
                log.info(
                    "Service exception handled: {}", result.getResponse().getContentAsString()));

    verify(videoService).compress(USER_EMAIL, BUCKET_KEY);
  }

  @Test
  void should_compress_video_with_special_characters_in_email() throws Exception {
    var specialEmail = "test+user@example.com";
    var jobId = UUID.randomUUID().toString();
    VideoCompressionJobStatusResponse expectedResponse =
        new VideoCompressionJobStatusResponse(
            jobId, ProcessStatus.PENDING, LocalDateTime.now(), null, null, null, null, 0);

    when(videoService.compress(specialEmail, BUCKET_KEY)).thenReturn(expectedResponse);

    mvc.perform(
            post(BASE_URL + "/compress/{userEmail}", specialEmail)
                .param("bucket_key", BUCKET_KEY)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.job_id").value(jobId))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andDo(
            result ->
                log.info(
                    "Compression request successful for email with special chars: {}",
                    result.getResponse().getContentAsString()));

    verify(videoService).compress(specialEmail, BUCKET_KEY);
  }

  @Test
  void should_compress_video_with_special_characters_in_bucket_key() throws Exception {
    var specialBucketKey = "videos/user_123/video-name_2024.mp4";
    var jobId = UUID.randomUUID().toString();
    var expectedResponse =
        new VideoCompressionJobStatusResponse(
            jobId, ProcessStatus.PENDING, LocalDateTime.now(), null, null, null, null, 0);

    when(videoService.compress(USER_EMAIL, specialBucketKey)).thenReturn(expectedResponse);

    mvc.perform(
            post(BASE_URL + "/compress/{userEmail}", USER_EMAIL)
                .param("bucket_key", specialBucketKey)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.job_id").value(jobId))
        .andDo(
            result ->
                log.info(
                    "Compression request successful for bucket_key with special chars: {}",
                    result.getResponse().getContentAsString()));

    verify(videoService).compress(USER_EMAIL, specialBucketKey);
  }

  @Test
  void should_get_compression_job_status_by_job_id() throws Exception {
    var jobId = UUID.randomUUID().toString();
    var expectedResponse =
        new VideoCompressionJobStatusResponse(
            jobId,
            ProcessStatus.COMPLETED,
            LocalDateTime.now().minusMinutes(5),
            LocalDateTime.now(),
            UUID.randomUUID().toString(),
            "s3://bucket/compressed_video.mp4",
            null,
            1);

    when(videoService.getCompressionStatus(jobId)).thenReturn(expectedResponse);

    mvc.perform(
            get(BASE_URL + "/compression-status/{jobId}", jobId)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.job_id").value(jobId))
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.created_at").exists())
        .andExpect(jsonPath("$.completed_at").exists())
        .andExpect(jsonPath("$.compressed_video_id").exists())
        .andExpect(jsonPath("$.compressed_video_url").value("s3://bucket/compressed_video.mp4"))
        .andExpect(jsonPath("$.attempt_count").value(1))
        .andDo(
            result ->
                log.info(
                    "Retrieved compression status: {}", result.getResponse().getContentAsString()));

    verify(videoService).getCompressionStatus(jobId);
  }

  @Test
  void should_return_not_found_when_job_id_does_not_exist() throws Exception {
    var nonExistentJobId = UUID.randomUUID().toString();
    when(videoService.getCompressionStatus(nonExistentJobId))
        .thenThrow(new EntityNotFoundException("Compression job not found: " + nonExistentJobId));

    mvc.perform(
            get(BASE_URL + "/compression-status/{jobId}", nonExistentJobId)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andDo(
            result ->
                log.info(
                    "Job not found as expected: {}", result.getResponse().getContentAsString()));

    verify(videoService).getCompressionStatus(nonExistentJobId);
  }

  @Test
  void should_get_compression_job_with_progressing_status() throws Exception {
    var jobId = UUID.randomUUID().toString();
    var expectedResponse =
        new VideoCompressionJobStatusResponse(
            jobId,
            ProcessStatus.PROGRESSING,
            LocalDateTime.now().minusMinutes(2),
            null,
            null,
            null,
            null,
            1);

    when(videoService.getCompressionStatus(jobId)).thenReturn(expectedResponse);

    mvc.perform(
            get(BASE_URL + "/compression-status/{jobId}", jobId)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.job_id").value(jobId))
        .andExpect(jsonPath("$.status").value("PROGRESSING"))
        .andExpect(jsonPath("$.completed_at").doesNotExist())
        .andExpect(jsonPath("$.compressed_video_id").doesNotExist())
        .andExpect(jsonPath("$.attempt_count").value(1))
        .andDo(
            result ->
                log.info("Job still progressing: {}", result.getResponse().getContentAsString()));

    verify(videoService).getCompressionStatus(jobId);
  }

  @Test
  void should_get_compression_job_with_failed_status() throws Exception {
    String jobId = UUID.randomUUID().toString();
    VideoCompressionJobStatusResponse expectedResponse =
        new VideoCompressionJobStatusResponse(
            jobId,
            ProcessStatus.FAILED,
            LocalDateTime.now().minusMinutes(10),
            LocalDateTime.now().minusMinutes(5),
            null,
            null,
            "FFmpeg processing failed on attempt 3",
            3);

    when(videoService.getCompressionStatus(jobId)).thenReturn(expectedResponse);

    mvc.perform(
            get(BASE_URL + "/compression-status/{jobId}", jobId)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.job_id").value(jobId))
        .andExpect(jsonPath("$.status").value("FAILED"))
        .andExpect(jsonPath("$.error_message").value("FFmpeg processing failed on attempt 3"))
        .andExpect(jsonPath("$.attempt_count").value(3))
        .andDo(
            result ->
                log.info("Job failed as expected: {}", result.getResponse().getContentAsString()));

    verify(videoService).getCompressionStatus(jobId);
  }

  @Test
  void should_get_all_compression_jobs_for_video() throws Exception {
    var videoId = UUID.randomUUID().toString();
    var jobs =
        List.of(
            new VideoCompressionJobStatusResponse(
                UUID.randomUUID().toString(),
                ProcessStatus.COMPLETED,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                UUID.randomUUID().toString(),
                "s3://bucket/compressed1.mp4",
                null,
                1),
            new VideoCompressionJobStatusResponse(
                UUID.randomUUID().toString(),
                ProcessStatus.FAILED,
                LocalDateTime.now().minusMinutes(30),
                LocalDateTime.now().minusMinutes(25),
                null,
                null,
                "Upload failed",
                2),
            new VideoCompressionJobStatusResponse(
                UUID.randomUUID().toString(),
                ProcessStatus.PROGRESSING,
                LocalDateTime.now().minusMinutes(5),
                null,
                null,
                null,
                null,
                1));

    when(videoService.getCompressionJobsByVideoId(videoId)).thenReturn(jobs);

    mvc.perform(
            get(BASE_URL + "/compression-jobs/{videoId}", videoId)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].status").value("COMPLETED"))
        .andExpect(jsonPath("$[1].status").value("FAILED"))
        .andExpect(jsonPath("$[1].error_message").value("Upload failed"))
        .andExpect(jsonPath("$[2].status").value("PROGRESSING"))
        .andDo(
            result ->
                log.info(
                    "Retrieved all compression jobs for video: {}",
                    result.getResponse().getContentAsString()));

    verify(videoService).getCompressionJobsByVideoId(videoId);
  }

  private List<Video> createMockCompressedVideos() {
    List<Video> videos = new ArrayList<>();

    for (int i = 1; i <= 3; i++) {
      Video video = new Video();
      video.setId(UUID.randomUUID().toString());
      video.setFileName("compressed_video_" + i + ".mp4");
      video.setFilePath("s3://bucket/compressed_" + i + ".mp4");
      video.setWidth(1280);
      video.setHeight(720);
      video.setDuration(120.0);
      video.setFrameRate(30.0);
      video.setSize(5_000_000L);
      video.setSizeType(SizeType.BYTES);
      video.setFileType(FileType.VIDEO);
      video.setCodec(VideoCodec.H264);
      video.setContainerFormat(ContainerFormat.MP4);
      video.setAudioChannels(2);
      video.setAudioSampleRate(48000);
      video.setAudioCodec(AudioCodec.AAC);
      video.setCreatedAt(LocalDateTime.now());
      videos.add(video);
    }

    return videos;
  }
}
