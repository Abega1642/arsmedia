package dev.razafindratelo.arsmedia.service;

import dev.razafindratelo.arsmedia.exception.MultipartFileConversionException;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Slf4j
public class MultipartFileConverter implements Function<MultipartFile, File> {

  @Override
  public File apply(MultipartFile multipartFile) {
    try {
      return convert(multipartFile);
    } catch (IOException e) {
      throw new MultipartFileConversionException("Multipart file conversion to file failed", e);
    }
  }

  public File convert(@NotNull MultipartFile multipartFile) throws IOException {
    String originalName = multipartFile.getOriginalFilename();
    String suffix = extractFileSuffix(originalName);

    File tempFile = File.createTempFile("upload-", suffix);
    multipartFile.transferTo(tempFile);
    tempFile.deleteOnExit();

    log.info("Converted multipart file to temporary file: {}", tempFile.getAbsolutePath());

    return tempFile;
  }

  private String extractFileSuffix(String filename) {
    if (filename == null || !filename.contains(".")) {
      return ".tmp";
    }
    return filename.substring(filename.lastIndexOf('.'));
  }
}
