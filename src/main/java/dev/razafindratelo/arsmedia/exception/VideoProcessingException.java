package dev.razafindratelo.arsmedia.exception;

public class VideoProcessingException extends RuntimeException {
  public VideoProcessingException(String message) {
    super(message);
  }

  public VideoProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
