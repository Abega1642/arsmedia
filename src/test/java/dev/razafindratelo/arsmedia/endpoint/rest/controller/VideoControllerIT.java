package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job.AudioExtractionJobStatusResponse;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job.VideoCompressionJobStatusResponse;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job.VideoFormatConversionJobStatusResponse;
import dev.razafindratelo.arsmedia.exception.InvalidFormatConversionException;
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
  private static final String USER_EMAIL_PARAM = "userEmail";
  private static final String COMPRESS_ENDPOINT = "/compress/";
  private static final String EXTRACT_AUDIO_ENDPOINT = "/extract-audio/";
  private static final String FORMAT_CONVERT_ENDPOINT = "/format-convert/";
  private static final String FORMAT_CONVERSION_STATUS = "/format-conversion-status/";

  @Autowired private MockMvc mvc;
  @MockitoBean private VideoService videoService;
  @MockitoBean private CompressionVideoService compressionService;

  @Test
  void should_compress_video_and_return_job_status() throws Exception {
    var jobId = randomUUID().toString();
    var expected =
        buildJobStatus(
            jobId, ProcessStatus.PENDING, LocalDateTime.now(), null, null, null, null, 0);
    when(videoService.compress(USER_EMAIL, BUCKET_KEY)).thenReturn(expected);

    var result =
        postTreatment(USER_EMAIL, BUCKET_KEY, COMPRESS_ENDPOINT)
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
    var result =
        postTreatment(invalidEmail, BUCKET_KEY, COMPRESS_ENDPOINT)
            .andExpect(status().isBadRequest());
    logResponse(result, "Validation failed for invalid email");
    verify(videoService, never()).compress(anyString(), anyString());
  }

  @Test
  void should_return_bad_request_when_user_email_is_missing() throws Exception {
    var result =
        postTreatment(null, BUCKET_KEY, COMPRESS_ENDPOINT).andExpect(status().isBadRequest());
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

    var result =
        postTreatment(USER_EMAIL, nonExistentBucketKey, COMPRESS_ENDPOINT)
            .andExpect(status().isNotFound());
    logResponse(result, "Entity not found as expected");
    verify(videoService).compress(USER_EMAIL, nonExistentBucketKey);
  }

  @Test
  void should_return_bad_request_when_email_is_blank() throws Exception {
    var blankEmail = "   ";
    var result =
        postTreatment(blankEmail, BUCKET_KEY, COMPRESS_ENDPOINT).andExpect(status().isBadRequest());
    logResponse(result, "Validation failed for blank email");
    verify(videoService, never()).compress(anyString(), anyString());
  }

  @Test
  void should_return_bad_request_when_bucket_key_is_blank() throws Exception {
    var blankBucketKey = "   ";
    var result =
        postTreatment(USER_EMAIL, blankBucketKey, COMPRESS_ENDPOINT)
            .andExpect(status().isBadRequest());
    logResponse(result, "Validation failed for blank bucket_key");
    verify(videoService, never()).compress(anyString(), anyString());
  }

  @Test
  void should_handle_service_exception_gracefully() throws Exception {
    when(videoService.compress(USER_EMAIL, BUCKET_KEY))
        .thenThrow(new RuntimeException("Unexpected service error"));

    var result =
        postTreatment(USER_EMAIL, BUCKET_KEY, COMPRESS_ENDPOINT)
            .andExpect(status().is5xxServerError());
    logResponse(result, "Service exception handled");
    verify(videoService).compress(USER_EMAIL, BUCKET_KEY);
  }

  @Test
  void should_compress_video_with_special_characters_in_email() throws Exception {
    var specialEmail = "test+user@example.com";
    var jobId = randomUUID().toString();
    var expected =
        buildJobStatus(
            jobId, ProcessStatus.PENDING, LocalDateTime.now(), null, null, null, null, 0);
    when(videoService.compress(specialEmail, BUCKET_KEY)).thenReturn(expected);

    var result =
        postTreatment(specialEmail, BUCKET_KEY, COMPRESS_ENDPOINT)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.job_id").value(jobId))
            .andExpect(jsonPath("$.status").value("PENDING"));

    logResponse(result, "Compression request for email with special chars successful");
    verify(videoService).compress(specialEmail, BUCKET_KEY);
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
    var jobId = randomUUID().toString();
    var expected =
        buildJobStatus(
            jobId,
            ProcessStatus.COMPLETED,
            LocalDateTime.now().minusMinutes(5),
            LocalDateTime.now(),
            randomUUID().toString(),
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
    var jobId = randomUUID().toString();
    when(videoService.getCompressionStatus(jobId))
        .thenThrow(new EntityNotFoundException("Compression job not found: " + jobId));

    var result = getCompressionStatus(jobId).andExpect(status().isNotFound());
    logResponse(result, "Job not found as expected");
    verify(videoService).getCompressionStatus(jobId);
  }

  @Test
  void should_get_compression_job_with_progressing_status() throws Exception {
    var jobId = randomUUID().toString();
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
    var jobId = randomUUID().toString();
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
    var videoId = randomUUID().toString();
    var jobs =
        List.of(
            buildJobStatus(
                randomUUID().toString(),
                ProcessStatus.COMPLETED,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                randomUUID().toString(),
                "s3://bucket/compressed1.mp4",
                null,
                1),
            buildJobStatus(
                randomUUID().toString(),
                ProcessStatus.FAILED,
                LocalDateTime.now().minusMinutes(30),
                LocalDateTime.now().minusMinutes(25),
                null,
                null,
                "Upload failed",
                2),
            buildJobStatus(
                randomUUID().toString(),
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

  @Test
  void should_extract_audio_and_return_job_status() throws Exception {
    var jobId = randomUUID().toString();
    var expected =
        buildAudioJobStatus(
            jobId, ProcessStatus.PENDING, LocalDateTime.now(), null, null, null, null, 0);
    when(videoService.extractAudio(USER_EMAIL, BUCKET_KEY)).thenReturn(expected);

    var result =
        postTreatment(USER_EMAIL, BUCKET_KEY, EXTRACT_AUDIO_ENDPOINT)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.job_id").value(jobId))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.created_at").exists())
            .andExpect(jsonPath("$.completed_at").doesNotExist())
            .andExpect(jsonPath("$.extracted_audio_id").doesNotExist())
            .andExpect(jsonPath("$.extracted_audio_bucket_key").doesNotExist())
            .andExpect(jsonPath("$.error_message").doesNotExist())
            .andExpect(jsonPath("$.attempt_count").value(0));

    logResponse(result, "Audio extraction request successful");
    verify(videoService).extractAudio(USER_EMAIL, BUCKET_KEY);
  }

  @Test
  void should_return_bad_request_when_email_is_invalid_for_audio_extraction() throws Exception {
    var invalidEmail = "invalid-email";
    var result =
        postTreatment(invalidEmail, BUCKET_KEY, EXTRACT_AUDIO_ENDPOINT)
            .andExpect(status().isBadRequest());
    logResponse(result, "Validation failed for invalid email in audio extraction");
    verify(videoService, never()).extractAudio(anyString(), anyString());
  }

  @Test
  void should_return_bad_request_when_user_email_is_missing_for_audio_extraction()
      throws Exception {
    var result =
        postTreatment(null, BUCKET_KEY, EXTRACT_AUDIO_ENDPOINT).andExpect(status().isBadRequest());
    logResponse(result, "Validation failed for missing bucket_key in audio extraction");
    verify(videoService, never()).extractAudio(anyString(), anyString());
  }

  @Test
  void should_return_not_found_when_video_does_not_exist_for_audio_extraction() throws Exception {
    var nonExistentBucketKey = "non_existent_key";
    when(videoService.extractAudio(USER_EMAIL, nonExistentBucketKey))
        .thenThrow(
            new EntityNotFoundException(
                "No video instance found with bucket_key = " + nonExistentBucketKey));

    var result =
        postTreatment(USER_EMAIL, nonExistentBucketKey, EXTRACT_AUDIO_ENDPOINT)
            .andExpect(status().isNotFound());
    logResponse(result, "Entity not found for audio extraction as expected");
    verify(videoService).extractAudio(USER_EMAIL, nonExistentBucketKey);
  }

  @Test
  void should_get_audio_extraction_job_status_by_job_id() throws Exception {
    var jobId = randomUUID().toString();
    var expected =
        buildAudioJobStatus(
            jobId,
            ProcessStatus.COMPLETED,
            LocalDateTime.now().minusMinutes(5),
            LocalDateTime.now(),
            randomUUID().toString(),
            "s3://bucket/extracted_audio.mp3",
            null,
            1);
    when(videoService.getAudioExtractionStatus(jobId)).thenReturn(expected);

    var result =
        getAudioExtractionStatus(jobId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.job_id").value(jobId))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(
                jsonPath("$.extracted_audio_bucket_key").value("s3://bucket/extracted_audio.mp3"));

    logResponse(result, "Retrieved audio extraction status");
    verify(videoService).getAudioExtractionStatus(jobId);
  }

  @Test
  void should_return_not_found_when_audio_extraction_job_id_does_not_exist() throws Exception {
    var jobId = randomUUID().toString();
    when(videoService.getAudioExtractionStatus(jobId))
        .thenThrow(new EntityNotFoundException("Audio extraction job not found: " + jobId));

    var result = getAudioExtractionStatus(jobId).andExpect(status().isNotFound());
    logResponse(result, "Audio extraction job not found as expected");
    verify(videoService).getAudioExtractionStatus(jobId);
  }

  @Test
  void should_get_audio_extraction_job_with_progressing_status() throws Exception {
    var jobId = randomUUID().toString();
    var expected =
        buildAudioJobStatus(
            jobId,
            ProcessStatus.PROGRESSING,
            LocalDateTime.now().minusMinutes(2),
            null,
            null,
            null,
            null,
            1);
    when(videoService.getAudioExtractionStatus(jobId)).thenReturn(expected);

    var result =
        getAudioExtractionStatus(jobId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.job_id").value(jobId))
            .andExpect(jsonPath("$.status").value("PROGRESSING"))
            .andExpect(jsonPath("$.completed_at").doesNotExist())
            .andExpect(jsonPath("$.extracted_audio_id").doesNotExist())
            .andExpect(jsonPath("$.attempt_count").value(1));

    logResponse(result, "Audio extraction job still progressing");
    verify(videoService).getAudioExtractionStatus(jobId);
  }

  @Test
  void should_get_audio_extraction_job_with_failed_status() throws Exception {
    var jobId = randomUUID().toString();
    var expected =
        buildAudioJobStatus(
            jobId,
            ProcessStatus.FAILED,
            LocalDateTime.now().minusMinutes(10),
            LocalDateTime.now().minusMinutes(5),
            null,
            null,
            "FFmpeg audio extraction failed on attempt 3",
            3);
    when(videoService.getAudioExtractionStatus(jobId)).thenReturn(expected);

    var result =
        getAudioExtractionStatus(jobId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.job_id").value(jobId))
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(
                jsonPath("$.error_message").value("FFmpeg audio extraction failed on attempt 3"))
            .andExpect(jsonPath("$.attempt_count").value(3));

    logResponse(result, "Audio extraction job failed as expected");
    verify(videoService).getAudioExtractionStatus(jobId);
  }

  @Test
  void should_convert_video_format_and_return_job_status() throws Exception {
    var jobId = randomUUID().toString();
    var expected =
        buildFormatConversionJobStatus(
            jobId, ProcessStatus.PENDING, LocalDateTime.now(), null, null, null, null, 0);
    when(videoService.convertTo(ContainerFormat.MKV, BUCKET_KEY, USER_EMAIL)).thenReturn(expected);

    var result =
        postFormatConversion(USER_EMAIL, BUCKET_KEY, ContainerFormat.MKV)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.job_id").value(jobId))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.created_at").exists())
            .andExpect(jsonPath("$.completed_at").doesNotExist())
            .andExpect(jsonPath("$.converted_video_id").doesNotExist())
            .andExpect(jsonPath("$.converted_video_bucket_key").doesNotExist())
            .andExpect(jsonPath("$.error_message").doesNotExist())
            .andExpect(jsonPath("$.attempt_count").value(0));

    logResponse(result, "Format conversion request successful");
    verify(videoService).convertTo(ContainerFormat.MKV, BUCKET_KEY, USER_EMAIL);
  }

  @Test
  void should_convert_video_to_webm_format() throws Exception {
    var jobId = randomUUID().toString();
    var expected =
        buildFormatConversionJobStatus(
            jobId, ProcessStatus.PENDING, LocalDateTime.now(), null, null, null, null, 0);
    when(videoService.convertTo(ContainerFormat.WEBM, BUCKET_KEY, USER_EMAIL)).thenReturn(expected);

    var result =
        postFormatConversion(USER_EMAIL, BUCKET_KEY, ContainerFormat.WEBM)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.job_id").value(jobId))
            .andExpect(jsonPath("$.status").value("PENDING"));

    logResponse(result, "WEBM format conversion request successful");
    verify(videoService).convertTo(ContainerFormat.WEBM, BUCKET_KEY, USER_EMAIL);
  }

  @Test
  void should_convert_video_to_various_formats() throws Exception {
    List<ContainerFormat> formats =
        List.of(
            ContainerFormat.MKV,
            ContainerFormat.AVI,
            ContainerFormat.FLV,
            ContainerFormat.MOV,
            ContainerFormat.THREEGP);

    for (ContainerFormat format : formats) {
      var jobId = randomUUID().toString();
      var expected =
          buildFormatConversionJobStatus(
              jobId, ProcessStatus.PENDING, LocalDateTime.now(), null, null, null, null, 0);
      when(videoService.convertTo(format, BUCKET_KEY, USER_EMAIL)).thenReturn(expected);

      var result =
          postFormatConversion(USER_EMAIL, BUCKET_KEY, format)
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.job_id").value(jobId))
              .andExpect(jsonPath("$.status").value("PENDING"));

      logResponse(result, format + " format conversion request successful");
      verify(videoService).convertTo(format, BUCKET_KEY, USER_EMAIL);
    }
  }

  @Test
  void should_return_bad_request_when_email_is_invalid_for_format_conversion() throws Exception {
    var invalidEmail = "invalid-email";
    var result =
        postFormatConversion(invalidEmail, BUCKET_KEY, ContainerFormat.MKV)
            .andExpect(status().isBadRequest());
    logResponse(result, "Validation failed for invalid email in format conversion");
    verify(videoService, never()).convertTo(any(), anyString(), anyString());
  }

  @Test
  void should_return_bad_request_when_user_email_is_missing_for_format_conversion()
      throws Exception {
    var result =
        postFormatConversion(null, BUCKET_KEY, ContainerFormat.MKV)
            .andExpect(status().isBadRequest());
    logResponse(result, "Validation failed for missing email in format conversion");
    verify(videoService, never()).convertTo(any(), anyString(), anyString());
  }

  @Test
  void should_return_bad_request_when_target_format_is_missing() throws Exception {
    var result =
        postFormatConversion(USER_EMAIL, BUCKET_KEY, null).andExpect(status().isBadRequest());
    logResponse(result, "Validation failed for missing target format");
    verify(videoService, never()).convertTo(any(), anyString(), anyString());
  }

  @Test
  void should_return_bad_request_when_bucket_key_is_blank_for_format_conversion() throws Exception {
    var blankBucketKey = "   ";
    var result =
        postFormatConversion(USER_EMAIL, blankBucketKey, ContainerFormat.MKV)
            .andExpect(status().isBadRequest());
    logResponse(result, "Validation failed for blank bucket_key in format conversion");
    verify(videoService, never()).convertTo(any(), anyString(), anyString());
  }

  @Test
  void should_return_not_found_when_video_does_not_exist_for_format_conversion() throws Exception {
    var nonExistentBucketKey = "non_existent_key";
    when(videoService.convertTo(ContainerFormat.MKV, nonExistentBucketKey, USER_EMAIL))
        .thenThrow(
            new EntityNotFoundException(
                "No video instance found with bucket_key = " + nonExistentBucketKey));

    var result =
        postFormatConversion(USER_EMAIL, nonExistentBucketKey, ContainerFormat.MKV)
            .andExpect(status().isNotFound());
    logResponse(result, "Entity not found for format conversion as expected");
    verify(videoService).convertTo(ContainerFormat.MKV, nonExistentBucketKey, USER_EMAIL);
  }

  @Test
  void should_return_bad_request_when_converting_video_to_audio_format() throws Exception {
    when(videoService.convertTo(ContainerFormat.MP3, BUCKET_KEY, USER_EMAIL))
        .thenThrow(
            new InvalidFormatConversionException(
                "Cannot convert video format MP4 to audio format MP3. Use audio extraction"
                    + " instead."));

    var result =
        postFormatConversion(USER_EMAIL, BUCKET_KEY, ContainerFormat.MP3)
            .andExpect(status().isBadRequest());
    logResponse(result, "Validation failed for video to audio format conversion");
    verify(videoService).convertTo(ContainerFormat.MP3, BUCKET_KEY, USER_EMAIL);
  }

  @Test
  void should_return_bad_request_when_source_and_target_formats_are_same() throws Exception {
    when(videoService.convertTo(ContainerFormat.MP4, BUCKET_KEY, USER_EMAIL))
        .thenThrow(
            new InvalidFormatConversionException("Source and target formats are the same: MP4"));

    var result =
        postFormatConversion(USER_EMAIL, BUCKET_KEY, ContainerFormat.MP4)
            .andExpect(status().isBadRequest());
    logResponse(result, "Validation failed for same format conversion");
    verify(videoService).convertTo(ContainerFormat.MP4, BUCKET_KEY, USER_EMAIL);
  }

  @Test
  void should_return_bad_request_when_target_format_is_unknown() throws Exception {
    when(videoService.convertTo(ContainerFormat.UNKNOWN, BUCKET_KEY, USER_EMAIL))
        .thenThrow(new InvalidFormatConversionException("Target format is unknown or unsupported"));

    var result =
        postFormatConversion(USER_EMAIL, BUCKET_KEY, ContainerFormat.UNKNOWN)
            .andExpect(status().isBadRequest());
    logResponse(result, "Validation failed for unknown target format");
    verify(videoService).convertTo(ContainerFormat.UNKNOWN, BUCKET_KEY, USER_EMAIL);
  }

  @Test
  void should_handle_service_exception_gracefully_for_format_conversion() throws Exception {
    when(videoService.convertTo(ContainerFormat.MKV, BUCKET_KEY, USER_EMAIL))
        .thenThrow(new RuntimeException("Unexpected service error"));

    var result =
        postFormatConversion(USER_EMAIL, BUCKET_KEY, ContainerFormat.MKV)
            .andExpect(status().is5xxServerError());
    logResponse(result, "Service exception handled for format conversion");
    verify(videoService).convertTo(ContainerFormat.MKV, BUCKET_KEY, USER_EMAIL);
  }

  @Test
  void should_get_format_conversion_job_status_by_job_id() throws Exception {
    var jobId = randomUUID().toString();
    var expected =
        buildFormatConversionJobStatus(
            jobId,
            ProcessStatus.COMPLETED,
            LocalDateTime.now().minusMinutes(5),
            LocalDateTime.now(),
            randomUUID().toString(),
            "s3://bucket/converted_video.mkv",
            null,
            1);
    when(videoService.getFormatConversionStatus(jobId)).thenReturn(expected);

    var result =
        getFormatConversionStatus(jobId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.job_id").value(jobId))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(
                jsonPath("$.converted_video_bucket_key").value("s3://bucket/converted_video.mkv"));

    logResponse(result, "Retrieved format conversion status");
    verify(videoService).getFormatConversionStatus(jobId);
  }

  @Test
  void should_return_not_found_when_format_conversion_job_id_does_not_exist() throws Exception {
    var jobId = randomUUID().toString();
    when(videoService.getFormatConversionStatus(jobId))
        .thenThrow(new EntityNotFoundException("Format conversion job not found: " + jobId));

    var result = getFormatConversionStatus(jobId).andExpect(status().isNotFound());
    logResponse(result, "Format conversion job not found as expected");
    verify(videoService).getFormatConversionStatus(jobId);
  }

  @Test
  void should_get_format_conversion_job_with_progressing_status() throws Exception {
    var jobId = randomUUID().toString();
    var expected =
        buildFormatConversionJobStatus(
            jobId,
            ProcessStatus.PROGRESSING,
            LocalDateTime.now().minusMinutes(2),
            null,
            null,
            null,
            null,
            1);
    when(videoService.getFormatConversionStatus(jobId)).thenReturn(expected);

    var result =
        getFormatConversionStatus(jobId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.job_id").value(jobId))
            .andExpect(jsonPath("$.status").value("PROGRESSING"))
            .andExpect(jsonPath("$.completed_at").doesNotExist())
            .andExpect(jsonPath("$.converted_video_id").doesNotExist())
            .andExpect(jsonPath("$.attempt_count").value(1));

    logResponse(result, "Format conversion job still progressing");
    verify(videoService).getFormatConversionStatus(jobId);
  }

  @Test
  void should_get_format_conversion_job_with_failed_status() throws Exception {
    var jobId = randomUUID().toString();
    var expected =
        buildFormatConversionJobStatus(
            jobId,
            ProcessStatus.FAILED,
            LocalDateTime.now().minusMinutes(10),
            LocalDateTime.now().minusMinutes(5),
            null,
            null,
            "FFmpeg format conversion failed on attempt 3",
            3);
    when(videoService.getFormatConversionStatus(jobId)).thenReturn(expected);

    var result =
        getFormatConversionStatus(jobId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.job_id").value(jobId))
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(
                jsonPath("$.error_message").value("FFmpeg format conversion failed on attempt 3"))
            .andExpect(jsonPath("$.attempt_count").value(3));

    logResponse(result, "Format conversion job failed as expected");
    verify(videoService).getFormatConversionStatus(jobId);
  }

  @Test
  void should_convert_video_with_special_characters_in_email() throws Exception {
    var specialEmail = "test+user@example.com";
    var jobId = randomUUID().toString();
    var expected =
        buildFormatConversionJobStatus(
            jobId, ProcessStatus.PENDING, LocalDateTime.now(), null, null, null, null, 0);
    when(videoService.convertTo(ContainerFormat.MKV, BUCKET_KEY, specialEmail))
        .thenReturn(expected);

    var result =
        postFormatConversion(specialEmail, BUCKET_KEY, ContainerFormat.MKV)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.job_id").value(jobId))
            .andExpect(jsonPath("$.status").value("PENDING"));

    logResponse(result, "Format conversion request for email with special chars successful");
    verify(videoService).convertTo(ContainerFormat.MKV, BUCKET_KEY, specialEmail);
  }

  private ResultActions postTreatment(String email, String bucketKey, String endpoint)
      throws Exception {
    MockHttpServletRequestBuilder request =
        post(BASE_URL + endpoint + "{bucketKey}", bucketKey)
            .contentType(MediaType.APPLICATION_JSON);
    if (bucketKey != null) request.param(USER_EMAIL_PARAM, email);
    return mvc.perform(request);
  }

  private ResultActions getCompressedVideos(String email) throws Exception {
    MockHttpServletRequestBuilder request =
        get(BASE_URL + "/compressed").contentType(MediaType.APPLICATION_JSON);
    if (email != null) request.param(USER_EMAIL_PARAM, email);
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
    return VideoCompressionJobStatusResponse.builder()
        .jobId(jobId)
        .status(status)
        .createdAt(createdAt)
        .completedAt(completedAt)
        .compressedVideoId(compressedVideoId)
        .compressedVideoUrl(compressedVideoUrl)
        .errorMessage(errorMessage)
        .attemptCount(attemptCount)
        .build();
  }

  private List<Video> createMockCompressedVideos() {
    List<Video> videos = new ArrayList<>();
    for (int i = 1; i <= 3; i++) {
      var mp4Suffix = ".mp4";
      Video v =
          Video.builder()
              .id(randomUUID().toString())
              .fileName("compressed_video_" + i + mp4Suffix)
              .filePath("s3://bucket/compressed_" + i + mp4Suffix)
              .width(1280)
              .height(720)
              .duration(120.0)
              .frameRate(30.0)
              .size(5_000_000L)
              .sizeType(SizeType.BYTES)
              .fileType(FileType.VIDEO)
              .codec(VideoCodec.H264)
              .containerFormat(ContainerFormat.MP4)
              .audioChannels(2)
              .audioSampleRate(48000)
              .audioCodec(AudioCodec.AAC)
              .createdAt(LocalDateTime.now())
              .build();
      videos.add(v);
    }
    return videos;
  }

  private ResultActions getAudioExtractionStatus(String jobId) throws Exception {
    return mvc.perform(
        get(BASE_URL + "/audio-extraction-status/{jobId}", jobId)
            .contentType(MediaType.APPLICATION_JSON));
  }

  private AudioExtractionJobStatusResponse buildAudioJobStatus(
      String jobId,
      ProcessStatus status,
      LocalDateTime createdAt,
      LocalDateTime completedAt,
      String extractedAudioId,
      String extractedAudioUrl,
      String errorMessage,
      int attemptCount) {
    return AudioExtractionJobStatusResponse.builder()
        .jobId(jobId)
        .status(status)
        .createdAt(createdAt)
        .completedAt(completedAt)
        .extractedAudioId(extractedAudioId)
        .extractedAudioBucketKey(extractedAudioUrl)
        .errorMessage(errorMessage)
        .attemptCount(attemptCount)
        .build();
  }

  private ResultActions postFormatConversion(
      String email, String bucketKey, ContainerFormat toFormat) throws Exception {
    MockHttpServletRequestBuilder request =
        post(BASE_URL + FORMAT_CONVERT_ENDPOINT + "{bucketKey}", bucketKey)
            .contentType(MediaType.APPLICATION_JSON);
    if (email != null) request.param("from", email);
    if (toFormat != null) request.param("to", toFormat.name());
    return mvc.perform(request);
  }

  private ResultActions getFormatConversionStatus(String jobId) throws Exception {
    return mvc.perform(
        get(BASE_URL + FORMAT_CONVERSION_STATUS + "{jobId}", jobId)
            .contentType(MediaType.APPLICATION_JSON));
  }

  private VideoFormatConversionJobStatusResponse buildFormatConversionJobStatus(
      String jobId,
      ProcessStatus status,
      LocalDateTime createdAt,
      LocalDateTime completedAt,
      String convertedVideoId,
      String convertedVideoBucketKey,
      String errorMessage,
      int attemptCount) {
    return VideoFormatConversionJobStatusResponse.builder()
        .jobId(jobId)
        .status(status)
        .createdAt(createdAt)
        .completedAt(completedAt)
        .convertedVideoId(convertedVideoId)
        .convertedVideoBucketKey(convertedVideoBucketKey)
        .errorMessage(errorMessage)
        .attemptCount(attemptCount)
        .build();
  }
}
