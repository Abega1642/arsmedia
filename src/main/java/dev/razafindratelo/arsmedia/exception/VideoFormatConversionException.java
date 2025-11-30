package dev.razafindratelo.arsmedia.exception;

public class VideoFormatConversionException extends RuntimeException {
  public VideoFormatConversionException(String message) {
    super(message);
  }

  public VideoFormatConversionException(String message, Throwable cause) {
    super(message, cause);
  }
}
