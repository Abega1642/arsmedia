package dev.razafindratelo.arsmedia.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TempFileCleaner {

  public void cleanUp(File... files) {
    if (isNothingToClean(files)) {
      log.debug("No temporary files to clean up");
      return;
    }

    CleanupResult result = deleteFiles(files);
    logCleanupSummary(result);
  }

  private boolean isNothingToClean(File[] files) {
    return files == null || files.length == 0;
  }

  private CleanupResult deleteFiles(File[] files) {
    int successCount = 0;
    int failureCount = 0;

    for (File file : files) {
      if (shouldSkipFile(file)) continue;

      if (deleteFile(file)) successCount++;
      else failureCount++;
    }

    return new CleanupResult(successCount, failureCount);
  }

  private boolean shouldSkipFile(File file) {
    if (file == null) return true;

    if (!file.exists()) {
      log.debug("File already deleted or doesn't exist: {}", file.getAbsolutePath());
      return true;
    }

    return false;
  }

  private boolean deleteFile(File file) {
    try {
      Files.delete(file.toPath());
      log.debug("Deleted temporary file: {}", file.getAbsolutePath());
      return true;
    } catch (IOException e) {
      log.warn(
          "Failed to delete temporary file: {}. Reason: {}",
          file.getAbsolutePath(),
          e.getMessage());
      return false;
    }
  }

  private void logCleanupSummary(CleanupResult result) {
    log.debug(
        "Cleanup completed: {} succeeded, {} failed", result.successCount(), result.failureCount());
  }

  private record CleanupResult(int successCount, int failureCount) {}
}
