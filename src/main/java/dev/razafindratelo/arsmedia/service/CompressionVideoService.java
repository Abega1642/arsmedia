package dev.razafindratelo.arsmedia.service;

import dev.razafindratelo.arsmedia.mapper.VideoMapper;
import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.repository.VideoCompressionJobRepository;
import dev.razafindratelo.arsmedia.repository.model.VideoCompressionJob;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@AllArgsConstructor
public class CompressionVideoService {
  private final VideoCompressionJobRepository repository;

  public List<Video> getAllCompressedVideos(@Email @NotBlank @NotNull String email) {
    var allCompressedVideos = repository.findAllCompletedByOwnerEmail(email);
    return allCompressedVideos.stream()
        .map(VideoCompressionJob::getParent)
        .map(VideoMapper::toVideo)
        .toList();
  }
}
