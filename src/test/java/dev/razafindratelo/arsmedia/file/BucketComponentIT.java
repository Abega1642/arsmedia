package dev.razafindratelo.arsmedia.file;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.razafindratelo.arsmedia.conf.FacadeIT;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

class BucketComponentIT extends FacadeIT {

  @TempDir Path tempDir;
  @Autowired private BucketComponent bucketComponent;
  private File testFile;
  private File testDirectory;

  @BeforeEach
  void setUp() throws IOException {
    testFile = tempDir.resolve("test-file.txt").toFile();
    Files.writeString(testFile.toPath(), "This is a test file content");

    testDirectory = tempDir.resolve("test-directory").toFile();
    assertTrue(testDirectory.mkdir(), "Failed to create test directory");

    var file1 = new File(testDirectory, "file1.txt");
    Files.writeString(file1.toPath(), "Content of file 1");

    var file2 = new File(testDirectory, "file2.txt");
    Files.writeString(file2.toPath(), "Content of file 2");

    var subDir = new File(testDirectory, "subdir");
    assertTrue(subDir.mkdir(), "Failed to create subdirectory");

    var file3 = new File(subDir, "file3.txt");
    Files.writeString(file3.toPath(), "Content of file 3 in subdirectory");
  }

  @Test
  void should_upload_directory() {
    var bucketKey = "test/directory";

    FileHash result = bucketComponent.upload(testDirectory, bucketKey);

    assertNotNull(result);
    assertEquals("NONE", result.algorithm());
    assertNull(result.value());
  }

  @Test
  void should_download_file_after_upload() throws IOException {
    var bucketKey = "test/download-test.txt";
    bucketComponent.upload(testFile, bucketKey);

    File downloadedFile = bucketComponent.download(bucketKey);

    assertNotNull(downloadedFile);
    assertTrue(downloadedFile.exists());
    String originalContent = Files.readString(testFile.toPath());
    String downloadedContent = Files.readString(downloadedFile.toPath());
    assertEquals(originalContent, downloadedContent);

    assertTrue(downloadedFile.delete(), "Failed to delete downloaded file");
  }

  @Test
  void should_upload_and_download_preserve_content() throws IOException {
    var bucketKey = "test/round-trip.txt";
    var originalContent = "Original content for round trip test";
    Files.writeString(testFile.toPath(), originalContent);

    bucketComponent.upload(testFile, bucketKey);
    File downloadedFile = bucketComponent.download(bucketKey);

    String downloadedContent = Files.readString(downloadedFile.toPath());
    assertEquals(originalContent, downloadedContent);

    assertTrue(downloadedFile.delete(), "Failed to delete downloaded file");
  }

  @Test
  void should_generate_presigned_url() {
    var bucketKey = "test/presign-test.txt";
    bucketComponent.upload(testFile, bucketKey);
    var expiration = Duration.ofHours(1);

    URL presignedUrl = bucketComponent.presign(bucketKey, expiration);

    assertNotNull(presignedUrl);
    String urlString = presignedUrl.toString();
    assertTrue(
        urlString.contains(bucketKey) || urlString.contains(bucketKey.replace("/", "%2F")),
        "Presigned URL should contain the bucket key");
  }

  @Test
  void should_upload_directory_with_nested_structure() throws IOException {
    var bucketKey = "test/nested-directory";
    bucketComponent.upload(testDirectory, bucketKey);

    File downloadedFile1 = bucketComponent.download(bucketKey + "/file1.txt");
    File downloadedFile2 = bucketComponent.download(bucketKey + "/file2.txt");
    File downloadedFile3 = bucketComponent.download(bucketKey + "/subdir/file3.txt");

    assertTrue(downloadedFile1.exists());
    assertTrue(downloadedFile2.exists());
    assertTrue(downloadedFile3.exists());

    assertEquals("Content of file 1", Files.readString(downloadedFile1.toPath()));
    assertEquals("Content of file 2", Files.readString(downloadedFile2.toPath()));
    assertEquals("Content of file 3 in subdirectory", Files.readString(downloadedFile3.toPath()));

    assertTrue(downloadedFile1.delete(), "Failed to delete downloaded file 1");
    assertTrue(downloadedFile2.delete(), "Failed to delete downloaded file 2");
    assertTrue(downloadedFile3.delete(), "Failed to delete downloaded file 3");
  }

  @Test
  void should_throw_exception_when_downloading_non_existent_file() {
    var nonExistentKey = "test/non-existent-file.txt";

    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> bucketComponent.download(nonExistentKey));
    assertTrue(exception.getMessage().contains("Download failed"));
  }

  @Test
  void should_handle_file_with_special_characters_in_name() throws IOException {
    var specialFile = tempDir.resolve("test-file-with-spaces and special.txt").toFile();
    Files.writeString(specialFile.toPath(), "Special content");
    var bucketKey = "test/special-chars.txt";

    FileHash result = bucketComponent.upload(specialFile, bucketKey);

    assertNotNull(result);
    assertEquals("SHA-256", result.algorithm());

    File downloaded = bucketComponent.download(bucketKey);
    assertEquals("Special content", Files.readString(downloaded.toPath()));
    assertTrue(downloaded.delete(), "Failed to delete downloaded file");
  }

  @Test
  void should_overwrite_existing_file_on_upload() throws IOException {
    var bucketKey = "test/overwrite-test.txt";
    Files.writeString(testFile.toPath(), "Original content");
    bucketComponent.upload(testFile, bucketKey);

    Files.writeString(testFile.toPath(), "Updated content");
    bucketComponent.upload(testFile, bucketKey);

    File downloaded = bucketComponent.download(bucketKey);
    assertEquals("Updated content", Files.readString(downloaded.toPath()));
    assertTrue(downloaded.delete(), "Failed to delete downloaded file");
  }

  @Test
  void should_handle_empty_file() throws IOException {
    var emptyFile = tempDir.resolve("empty.txt").toFile();
    Files.writeString(emptyFile.toPath(), "");
    var bucketKey = "test/empty-file.txt";

    FileHash result = bucketComponent.upload(emptyFile, bucketKey);

    assertNotNull(result);

    File downloaded = bucketComponent.download(bucketKey);
    assertEquals("", Files.readString(downloaded.toPath()));
    assertTrue(downloaded.delete(), "Failed to delete downloaded file");
  }

  @Test
  void should_handle_binary_file() throws IOException {
    var binaryFile = tempDir.resolve("binary.bin").toFile();
    byte[] binaryContent = new byte[] {0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE};
    Files.write(binaryFile.toPath(), binaryContent);
    var bucketKey = "test/binary-file.bin";

    FileHash result = bucketComponent.upload(binaryFile, bucketKey);

    assertNotNull(result);
    assertEquals("SHA-256", result.algorithm());

    File downloaded = bucketComponent.download(bucketKey);
    byte[] downloadedContent = Files.readAllBytes(downloaded.toPath());
    assertArrayEquals(binaryContent, downloadedContent);
    assertTrue(downloaded.delete(), "Failed to delete downloaded file");
  }
}
