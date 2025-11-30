package dev.razafindratelo.arsmedia.service.event;

import static dev.razafindratelo.arsmedia.mapper.MapperConstants.UTILITY_CLASS_EXCEPTION_MESSAGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import java.io.File;
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
        (mock, _) -> {
          FFmpegJob mockJob = mock(FFmpegJob.class);
          when(mock.createJob(any(FFmpegBuilder.class))).thenReturn(mockJob);

          doAnswer(
                  _ -> {
                    File systemTempDir = new File(System.getProperty("java.io.tmpdir"));
                    File[] outputFiles =
                        systemTempDir.listFiles(
                            (_, name) -> name.startsWith(prefix) && name.endsWith(extension));

                    if (outputFiles != null && outputFiles.length > 0) {
                      File actualOutputFile =
                          Arrays.stream(outputFiles)
                              .max(Comparator.comparingLong(File::lastModified))
                              .orElseThrow(
                                  () ->
                                      new RuntimeException(
                                          "Could not find output file with prefix: " + prefix));

                      log.info("Writing mock data to: {}", actualOutputFile.getAbsolutePath());

                      byte[] mockData = new byte[mockFileSize];
                      new Random().nextBytes(mockData);
                      Files.write(actualOutputFile.toPath(), mockData);

                      fileInterceptor.accept(actualOutputFile);

                      log.info("Successfully wrote {} bytes to file", actualOutputFile.length());
                    } else {
                      throw new RuntimeException(
                          "No temp file was created by the service with prefix: " + prefix);
                    }

                    return null;
                  })
              .when(mockJob)
              .run();
        });
  }
}
