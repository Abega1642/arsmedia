package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import dev.razafindratelo.arsmedia.service.VideoService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class VideoController {
  private VideoService service;
}
