package dev.razafindratelo.arsmedia.file;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TempFileCleanerTest {

  private final TempFileCleaner tempFileCleaner = new TempFileCleaner();

  @TempDir Path tempDir;

  @Test
  void should_delete_single_existing_file() throws IOException {
    var file = givenExistingFile("test0.txt");

    whenCleanUp(file);

    thenFileIsDeleted(file);
  }

  @Test
  void should_delete_multiple_files() throws IOException {
    File[] files = givenExistingFiles("test_1.txt", "test_2.txt", "test_3.txt");

    whenCleanUp(files);

    thenAllFilesAreDeleted(files);
  }

  @Test
  void should_delete_files_with_different_extensions() throws IOException {
    File[] files = givenExistingFiles("document.txt", "video.mp4", "audio.mp3");

    whenCleanUp(files);

    thenAllFilesAreDeleted(files);
  }

  @Test
  void should_delete_file_in_subdirectory() throws IOException {
    var file = givenFileInSubdirectory();

    whenCleanUp(file);

    thenFileIsDeleted(file);
  }

  @Test
  void should_delete_file_with_special_characters() throws IOException {
    var file = givenExistingFile("file-with_special.chars@2024.tmp");

    whenCleanUp(file);

    thenFileIsDeleted(file);
  }

  @Test
  void should_handle_null_file() {
    thenNoExceptionThrown(() -> tempFileCleaner.cleanUp((File) null));
  }

  @Test
  void should_handle_null_array() {
    thenNoExceptionThrown(() -> tempFileCleaner.cleanUp((File[]) null));
  }

  @Test
  void should_handle_empty_array() {
    thenNoExceptionThrown(tempFileCleaner::cleanUp);
  }

  @Test
  void should_handle_mixed_null_and_valid_files() throws IOException {
    var file1 = givenExistingFile("test1.txt");
    var file3 = givenExistingFile("test3.txt");

    whenCleanUp(file1, null, file3);

    thenFileIsDeleted(file1);
    thenFileIsDeleted(file3);
  }

  @Test
  void should_handle_all_null_files() {
    File[] allNulls = new File[] {null, null, null};

    thenNoExceptionThrown(() -> tempFileCleaner.cleanUp(allNulls));
  }

  @Test
  void should_handle_non_existent_file() {
    var nonExistent = givenNonExistentFile("nonexistent.txt");

    thenNoExceptionThrown(() -> tempFileCleaner.cleanUp(nonExistent));
  }

  @Test
  void should_handle_already_deleted_file() throws IOException {
    var file = givenExistingFile("test.txt");
    Files.delete(file.toPath());

    thenNoExceptionThrown(() -> tempFileCleaner.cleanUp(file));
  }

  @Test
  void should_handle_all_non_existent_files() {
    File[] nonExistent = givenNonExistentFiles("missing1.txt", "missing2.txt", "missing3.txt");

    thenNoExceptionThrown(() -> tempFileCleaner.cleanUp(nonExistent));
  }

  @Test
  void should_continue_deleting_when_one_file_fails() throws IOException {
    var validFile1 = givenExistingFile("valid1.txt");
    var readOnlyFile = givenReadOnlyFile();
    var validFile2 = givenExistingFile("valid2.txt");

    whenCleanUp(validFile1, readOnlyFile, validFile2);

    thenFileIsDeleted(validFile1);
    thenFileIsDeleted(validFile2);

    cleanupReadOnlyFile(readOnlyFile);
  }

  @Test
  void should_handle_mixed_success_and_failure_scenarios() throws IOException {
    var existingFile1 = givenExistingFile("test01.txt");
    var existingFile2 = givenExistingFile("test02.txt");
    var nonExistent = givenNonExistentFile("nonexistent.txt");

    whenCleanUp(existingFile1, nonExistent, null, existingFile2);

    thenFileIsDeleted(existingFile1);
    thenFileIsDeleted(existingFile2);
  }

  @Test
  void should_handle_large_number_of_files() throws IOException {
    File[] files = givenExistingFiles();

    whenCleanUp(files);

    thenAllFilesAreDeleted(files);
  }

  private File givenExistingFile(String filename) throws IOException {
    var file = tempDir.resolve(filename).toFile();
    assertTrue(file.createNewFile(), "Should create file: " + filename);
    return file;
  }

  private File[] givenExistingFiles(String... filenames) throws IOException {
    File[] files = new File[filenames.length];
    for (int i = 0; i < filenames.length; i++) {
      files[i] = givenExistingFile(filenames[i]);
    }
    return files;
  }

  private File[] givenExistingFiles() throws IOException {
    File[] files = new File[100];
    for (int i = 0; i < 100; i++) {
      files[i] = givenExistingFile("file_" + i + ".txt");
    }
    return files;
  }

  private File givenReadOnlyFile() throws IOException {
    var file = givenExistingFile("readonly.txt");
    assertTrue(file.setWritable(false), "Should make file read-only");
    return file;
  }

  private File givenFileInSubdirectory() throws IOException {
    Path filePath = tempDir.resolve("subdir/nested/file.txt");
    Files.createDirectories(filePath.getParent());
    var file = filePath.toFile();
    assertTrue(file.createNewFile(), "Should create file in subdirectory");
    return file;
  }

  private File givenNonExistentFile(String filename) {
    return new File(tempDir.toFile(), filename);
  }

  private File[] givenNonExistentFiles(String... filenames) {
    File[] files = new File[filenames.length];
    for (int i = 0; i < filenames.length; i++) {
      files[i] = givenNonExistentFile(filenames[i]);
    }
    return files;
  }

  private void whenCleanUp(File... files) {
    tempFileCleaner.cleanUp(files);
  }

  private void thenFileIsDeleted(File file) {
    assertFalse(file.exists(), "File should be deleted: " + file.getName());
  }

  private void thenAllFilesAreDeleted(File... files) {
    for (File file : files) {
      thenFileIsDeleted(file);
    }
  }

  private void thenNoExceptionThrown(Runnable action) {
    assertDoesNotThrow(action::run, "Should not throw any exception");
  }

  private void cleanupReadOnlyFile(File file) {
    file.setWritable(true);
    file.delete();
  }
}
