package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.ProcessSuccessResponse;
import dev.razafindratelo.arsmedia.service.VideoService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/media/videos")
public class VideoController {
  private VideoService service;

  @PostMapping("/compress/{userEmail}")
  public ProcessSuccessResponse compress(
      @PathVariable(name = "userEmail") String userEmail,
      @RequestParam(name = "bucket_key") @NotNull @NotBlank String bucketKey) {
    return service.compress(userEmail, bucketKey);
  }
}
