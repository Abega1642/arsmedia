package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.service.VideoService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/media/videos")
public class VideoController {
  private VideoService service;

  @PostMapping("/compress/{videoId}")
  public Video compress(
      @PathVariable(name = "videoId") String videoId,
      @RequestParam(name = "bucket_key") @NotNull @NotBlank String bucketKey) {
    throw new NotImplementedException();
  }
}
