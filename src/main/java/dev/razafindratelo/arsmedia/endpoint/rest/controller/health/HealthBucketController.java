package dev.razafindratelo.arsmedia.endpoint.rest.controller.health;

import dev.razafindratelo.arsmedia.InfraGenerated;
import dev.razafindratelo.arsmedia.file.BucketComponent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@InfraGenerated
@RestController
@AllArgsConstructor
public class HealthBucketController {

  private final BucketComponent bucketComponent;
  private static final String HEALTH_KEY = "health";

  @GetMapping("/health/bucket")
  public ResponseEntity<String> file_can_be_uploaded_then_signed() throws IOException {
    String fileSuffix = ".txt";
    String filePrefix = UUID.randomUUID().toString();
    File fileToUpload = Files.createTempFile(filePrefix, fileSuffix).toFile();
    writeRandomContent(fileToUpload);

    String fileBucketKey = HEALTH_KEY + "/" + filePrefix + fileSuffix;
    bucketComponent.upload(fileToUpload, fileBucketKey);

    File downloaded = bucketComponent.download(fileBucketKey);
    if (!Files.readString(fileToUpload.toPath()).equals(Files.readString(downloaded.toPath()))) {
      throw new RuntimeException("Uploaded and downloaded content mismatch");
    }

    String dirPrefix = "dir-" + UUID.randomUUID();
    File dir = Files.createTempDirectory(dirPrefix).toFile();
    File fInDir = new File(dir, UUID.randomUUID() + ".txt");
    writeRandomContent(fInDir);
    String dirBucketKey = HEALTH_KEY + "/" + dirPrefix;
    bucketComponent.upload(dir, dirBucketKey);

    return ResponseEntity.ok(
        bucketComponent.presign(fileBucketKey, Duration.ofMinutes(2)).toString());
  }

  private void writeRandomContent(File file) throws IOException {
    try (FileWriter writer = new FileWriter(file)) {
      writer.write(UUID.randomUUID().toString());
    }
  }
}
