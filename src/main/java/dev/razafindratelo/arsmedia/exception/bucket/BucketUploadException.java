package dev.razafindratelo.arsmedia.exception.bucket;

public class BucketUploadException extends BucketOperationException {

  public BucketUploadException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public String getErrorCode() {
    return "BUCKET_UPLOAD_FAILED";
  }
}
