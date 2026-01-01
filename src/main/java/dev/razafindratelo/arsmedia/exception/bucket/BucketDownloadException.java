package dev.razafindratelo.arsmedia.exception.bucket;

import dev.razafindratelo.arsmedia.InfraGenerated;

@InfraGenerated
public class BucketDownloadException extends BucketOperationException {

  public BucketDownloadException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public String getErrorCode() {
    return "BUCKET_DOWNLOAD_FAILED";
  }
}
