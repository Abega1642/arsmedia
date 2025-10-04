package dev.razafindratelo.arsmedia.file;

import dev.razafindratelo.arsmedia.InfraGenerated;
import dev.razafindratelo.arsmedia.config.BucketConf;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;

@InfraGenerated
@Component
@AllArgsConstructor
public class BucketComponent {

  private final BucketConf bucketConf;

  public FileHash upload(File file, String bucketKey) {
    if (file.isDirectory()) {
      return uploadDirectory(file, bucketKey);
    } else {
      return uploadFile(file, bucketKey);
    }
  }

  private FileHash uploadDirectory(File directory, String bucketKey) {
    try (Stream<Path> files = Files.walk(directory.toPath())) {
      files
          .filter(Files::isRegularFile)
          .forEach(
              path -> {
                String relativeKey =
                    bucketKey
                        + "/"
                        + directory.toPath().relativize(path).toString().replace("\\", "/");
                uploadFile(path.toFile(), relativeKey);
              });
    } catch (IOException e) {
      throw new RuntimeException("Failed to upload directory: " + directory, e);
    }
    return new FileHash("NONE", null);
  }

  private FileHash uploadFile(File file, String bucketKey) {
    try {
      var request =
          UploadFileRequest.builder()
              .source(file)
              .putObjectRequest(req -> req.bucket(bucketConf.getBucketName()).key(bucketKey))
              .addTransferListener(LoggingTransferListener.create())
              .build();

      var upload = bucketConf.getS3TransferManager().uploadFile(request);
      var completed = upload.completionFuture().join();
      return new FileHash("SHA-256", completed.response().checksumSHA256());
    } catch (Exception e) {
      throw new RuntimeException("Upload failed for key: " + bucketKey, e);
    }
  }

  public File download(String bucketKey) {
    try {
      File destination = File.createTempFile("b2-", "-" + bucketKey.replace("/", "-"));
      var request =
          DownloadFileRequest.builder()
              .getObjectRequest(
                  GetObjectRequest.builder()
                      .bucket(bucketConf.getBucketName())
                      .key(bucketKey)
                      .build())
              .destination(destination)
              .build();

      bucketConf.getS3TransferManager().downloadFile(request).completionFuture().join();
      return destination;
    } catch (Exception e) {
      throw new RuntimeException("Download failed for key: " + bucketKey, e);
    }
  }

  public URL presign(String bucketKey, Duration expiration) {
    var getObjectRequest =
        GetObjectRequest.builder().bucket(bucketConf.getBucketName()).key(bucketKey).build();

    return bucketConf
        .getS3Presigner()
        .presignGetObject(
            GetObjectPresignRequest.builder()
                .getObjectRequest(getObjectRequest)
                .signatureDuration(expiration)
                .build())
        .url();
  }
}
