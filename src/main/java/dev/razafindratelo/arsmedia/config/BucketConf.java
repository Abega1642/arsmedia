package dev.razafindratelo.arsmedia.config;

import dev.razafindratelo.arsmedia.InfraGenerated;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

@InfraGenerated
@Configuration
public class BucketConf {

  @Getter private final String bucketName;
  @Getter private final S3TransferManager s3TransferManager;
  @Getter private final S3Presigner s3Presigner;

  @SneakyThrows
  public BucketConf(
      @Value("${b2.key.id}") String keyId,
      @Value("${b2.application.key}") String applicationKey,
      @Value("${b2.bucket.name}") String bucketName,
      @Value("${b2.region}") String regionString,
      @Value("${b2.endpoint.prefix}") String endpointPrefix,
      @Value("${b2.endpoint.suffix}") String endpointSuffix,
      @Value("${b2.upload.part-size-mb:8}") int partSizeMb,
      @Value("${b2.upload.target-throughput-gbps:10.0}") double targetThroughputGbps) {

    this.bucketName = bucketName;

    String fullEndpoint =
        (endpointPrefix.contains("localhost") || endpointPrefix.contains("127.0.0.1"))
            ? endpointPrefix
            : endpointPrefix + regionString + endpointSuffix;
    URI endpoint = URI.create(fullEndpoint);
    Region region = Region.of(regionString);

    AwsCredentialsProvider credentialsProvider =
        StaticCredentialsProvider.create(AwsBasicCredentials.create(keyId, applicationKey));

    S3AsyncClient s3AsyncClient =
        S3AsyncClient.crtBuilder()
            .endpointOverride(endpoint)
            .region(region)
            .credentialsProvider(credentialsProvider)
            .targetThroughputInGbps(targetThroughputGbps)
            .minimumPartSizeInBytes((long) partSizeMb * 1024 * 1024)
            .build();

    this.s3TransferManager = S3TransferManager.builder().s3Client(s3AsyncClient).build();

    this.s3Presigner =
        S3Presigner.builder()
            .endpointOverride(endpoint)
            .region(region)
            .credentialsProvider(credentialsProvider)
            .build();
  }

  @PreDestroy
  public void cleanup() {
    if (s3TransferManager != null) {
      s3TransferManager.close();
    }
    if (s3Presigner != null) {
      s3Presigner.close();
    }
  }
}
