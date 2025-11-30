package dev.razafindratelo.arsmedia.service.event;

import static dev.razafindratelo.arsmedia.mapper.MapperConstants.UTILITY_CLASS_EXCEPTION_MESSAGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.mockito.MockedConstruction;

@Slf4j
public class FFmpegMockHelper {

  private FFmpegMockHelper() {
    throw new UnsupportedOperationException(UTILITY_CLASS_EXCEPTION_MESSAGE);
  }

  public static MockedConstruction<FFmpegExecutor> setupFfmpegMock(
      String prefix, String extension, int mockFileSize, Consumer<File> fileInterceptor) {

    return mockConstruction(
        FFmpegExecutor.class,
        (mock, _) -> setupMockBehavior(mock, prefix, extension, mockFileSize, fileInterceptor));
  }

  private static void setupMockBehavior(
      FFmpegExecutor mock,
      String prefix,
      String extension,
      int mockFileSize,
      Consumer<File> fileInterceptor) {

    FFmpegJob mockJob = mock(FFmpegJob.class);
    when(mock.createJob(any(FFmpegBuilder.class))).thenReturn(mockJob);

    doAnswer(
            _ -> {
              File output = findLatestTempFile(prefix, extension);
              writeMockData(output, mockFileSize);
              fileInterceptor.accept(output);
              return null;
            })
        .when(mockJob)
        .run();
  }

  private static File findLatestTempFile(String prefix, String extension) {
    File dir = new File(System.getProperty("java.io.tmpdir"));
    File[] matches =
        dir.listFiles((d, name) -> name.startsWith(prefix) && name.endsWith(extension));

    return Arrays.stream(matches == null ? new File[0] : matches)
        .max(Comparator.comparingLong(File::lastModified))
        .orElseThrow(() -> new RuntimeException("No temp file found for prefix: " + prefix));
  }

  private static void writeMockData(File file, int size) throws IOException {
    byte[] data = new byte[size];
    new Random().nextBytes(data);
    Files.write(file.toPath(), data);
    log.info("Wrote {} bytes to {}", size, file.getAbsolutePath());
  }
}
