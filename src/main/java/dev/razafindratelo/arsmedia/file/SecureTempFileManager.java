package dev.razafindratelo.arsmedia.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Component for creating secure temporary files with restricted permissions. This class addresses
 * security vulnerabilities related to temporary file creation by ensuring files are created with
 * owner-only permissions from the start, preventing local information disclosure.
 *
 * <p>Usage example:
 *
 * <pre>
 * File tempFile = secureTempFileManager.createSecureTempFile("myfile-", ".txt");
 * try {
 *   // Use the temp file
 * } finally {
 *   secureTempFileManager.deleteTempFile(tempFile);
 * }
 * </pre>
 */
@Slf4j
@Component
public class SecureTempFileManager {

  private static final String POSIX_OWNER_ONLY_PERMISSIONS = "rw-------";
  private static final String DEFAULT_PREFIX = "temp-";
  private static final String DEFAULT_SUFFIX = ".tmp";

  /**
   * Creates a secure temporary file with default prefix and suffix.
   *
   * @return a secure temporary file
   * @throws IOException if file creation fails
   */
  public File createSecureTempFile() throws IOException {
    return createSecureTempFile(DEFAULT_PREFIX, DEFAULT_SUFFIX);
  }

  /**
   * Creates a secure temporary file with the specified prefix and suffix. The file is created with
   * owner-only read/write permissions to prevent information disclosure.
   *
   * <p>Security considerations:
   *
   * <ul>
   *   <li>On POSIX systems: Created with rw------- (600) permissions
   *   <li>On Windows: Created in user-specific temp directory with appropriate ACLs
   *   <li>File is marked for deletion on JVM exit via deleteOnExit()
   * </ul>
   *
   * @param prefix the prefix string to be used in generating the file's name
   * @param suffix the suffix string to be used in generating the file's name
   * @return a secure temporary file
   * @throws IOException if file creation fails
   * @throws IllegalArgumentException if prefix or suffix is invalid
   */
  public File createSecureTempFile(String prefix, String suffix) throws IOException {
    validateParameters(prefix, suffix);

    try {
      return createWithPosixPermissions(prefix, suffix);
    } catch (UnsupportedOperationException e) {
      log.debug("POSIX permissions not supported, using standard file creation");
      return createWithStandardPermissions(prefix, suffix);
    }
  }

  /**
   * Creates a secure temporary file and writes the specified content to it.
   *
   * @param prefix the prefix string for the file name
   * @param suffix the suffix string for the file name
   * @param content the content to write to the file
   * @return a secure temporary file with the content written
   * @throws IOException if file creation or writing fails
   */
  public File createSecureTempFileWithContent(String prefix, String suffix, String content)
      throws IOException {
    File tempFile = createSecureTempFile(prefix, suffix);
    try {
      Files.writeString(tempFile.toPath(), content);
      log.debug("Written {} bytes to secure temp file: {}", content.length(), tempFile.toPath());
      return tempFile;
    } catch (IOException e) {
      // Clean up on failure
      deleteTempFile(tempFile);
      throw e;
    }
  }

  /**
   * Creates a secure temporary file and writes the specified byte content to it.
   *
   * @param prefix the prefix string for the file name
   * @param suffix the suffix string for the file name
   * @param content the byte content to write to the file
   * @return a secure temporary file with the content written
   * @throws IOException if file creation or writing fails
   */
  public File createSecureTempFileWithContent(String prefix, String suffix, byte[] content)
      throws IOException {
    File tempFile = createSecureTempFile(prefix, suffix);
    try {
      Files.write(tempFile.toPath(), content);
      log.debug("Written {} bytes to secure temp file: {}.", content.length, tempFile.toPath());
      return tempFile;
    } catch (IOException e) {
      deleteTempFile(tempFile);
      throw e;
    }
  }

  /**
   * Safely deletes a temporary file. This method handles null files and non-existent files
   * gracefully without throwing exceptions.
   *
   * @param file the file to delete, can be null
   * @return true if the file was successfully deleted, false otherwise
   */
  public boolean deleteTempFile(File file) {
    if (file == null) {
      log.debug("Attempted to delete null file, skipping");
      return false;
    }

    if (!file.exists()) {
      log.debug("File does not exist, skipping deletion: {}", file.getAbsolutePath());
      return false;
    }

    try {
      Files.delete(file.toPath());
      log.debug("Successfully deleted temporary file: {}", file.getAbsolutePath());
      return true;
    } catch (IOException e) {
      log.warn("Failed to delete temporary file: {}", file.getAbsolutePath(), e);
      return false;
    }
  }

  /**
   * Creates a temporary file with POSIX permissions (owner-only read/write).
   *
   * @param prefix the prefix string for the file name
   * @param suffix the suffix string for the file name
   * @return a secure temporary file
   * @throws IOException if file creation fails
   * @throws UnsupportedOperationException if POSIX permissions are not supported
   */
  private File createWithPosixPermissions(String prefix, String suffix) throws IOException {
    Set<PosixFilePermission> permissions =
        PosixFilePermissions.fromString(POSIX_OWNER_ONLY_PERMISSIONS);

    Path tempPath =
        Files.createTempFile(prefix, suffix, PosixFilePermissions.asFileAttribute(permissions));

    File tempFile = tempPath.toFile();
    tempFile.deleteOnExit();

    log.debug(
        "Created secure temp file with POSIX permissions ({}): {}",
        POSIX_OWNER_ONLY_PERMISSIONS,
        tempPath);
    return tempFile;
  }

  /**
   * Creates a temporary file using standard Java NIO methods. This is more secure than the legacy
   * File.createTempFile() method because:
   *
   * <ul>
   *   <li>On Windows: Files are created in user-specific temp directories with proper ACLs
   *   <li>On Unix: Respects umask and system security policies
   *   <li>Permissions are set atomically at creation time
   * </ul>
   *
   * @param prefix the prefix string for the file name
   * @param suffix the suffix string for the file name
   * @return a temporary file with system-default secure permissions
   * @throws IOException if file creation fails
   */
  private File createWithStandardPermissions(String prefix, String suffix) throws IOException {
    Path tempPath = Files.createTempFile(prefix, suffix);

    File tempFile = tempPath.toFile();
    tempFile.deleteOnExit();

    log.debug("Created temp file with standard permissions: {}", tempPath);
    return tempFile;
  }

  /**
   * Validates the prefix and suffix parameters.
   *
   * @param prefix the prefix to validate
   * @param suffix the suffix to validate
   * @throws IllegalArgumentException if parameters are invalid
   */
  private void validateParameters(String prefix, String suffix) {
    if (prefix == null || prefix.length() < 3)
      throw new IllegalArgumentException("Prefix must be at least 3 characters long");

    if (suffix == null) throw new IllegalArgumentException("Suffix cannot be null");
  }
}
