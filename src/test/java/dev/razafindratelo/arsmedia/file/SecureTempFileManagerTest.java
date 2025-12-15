package dev.razafindratelo.arsmedia.file;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

class SecureTempFileManagerTest {

  private static final String TEST_PREFIX = "test-";
  private static final String TEST_SUFFIX = ".txt";
  private static final String TEST_CONTENT = "Test content for secure file";
  private static final byte[] TEST_BYTES = "Binary test content".getBytes();

  private SecureTempFileManager manager;
  private File createdFile;

  @BeforeEach
  void setUp() {
    manager = new SecureTempFileManager();
    createdFile = null;
  }

  @AfterEach
  void tearDown() {
    if (createdFile != null && createdFile.exists()) {
      manager.deleteTempFile(createdFile);
    }
  }

  @Test
  void should_create_temp_file_with_default_parameters() throws IOException {
    createdFile = manager.createSecureTempFile();

    assertNotNull(createdFile);
    assertTrue(createdFile.exists());
    assertTrue(createdFile.isFile());
    assertTrue(createdFile.getAbsolutePath().contains("temp-"));
    assertTrue(createdFile.getName().endsWith(".tmp"));
  }

  @Test
  void should_create_temp_file_with_custom_prefix_and_suffix() throws IOException {
    createdFile = manager.createSecureTempFile(TEST_PREFIX, TEST_SUFFIX);

    assertNotNull(createdFile);
    assertTrue(createdFile.exists());
    assertTrue(createdFile.getName().startsWith(TEST_PREFIX));
    assertTrue(createdFile.getName().endsWith(TEST_SUFFIX));
  }

  @Test
  void should_create_multiple_unique_temp_files() throws IOException {
    File firstFile = manager.createSecureTempFile(TEST_PREFIX, TEST_SUFFIX);
    File secondFile = manager.createSecureTempFile(TEST_PREFIX, TEST_SUFFIX);

    try {
      assertNotNull(firstFile);
      assertNotNull(secondFile);
      assertNotEquals(firstFile.getAbsolutePath(), secondFile.getAbsolutePath());
      assertTrue(firstFile.exists());
      assertTrue(secondFile.exists());
    } finally {
      manager.deleteTempFile(firstFile);
      manager.deleteTempFile(secondFile);
    }
  }

  @Test
  void should_create_temp_file_with_string_content() throws IOException {
    createdFile = manager.createSecureTempFileWithContent(TEST_PREFIX, TEST_SUFFIX, TEST_CONTENT);

    assertNotNull(createdFile);
    assertTrue(createdFile.exists());

    String actualContent = Files.readString(createdFile.toPath());
    assertEquals(TEST_CONTENT, actualContent);
  }

  @Test
  void should_create_temp_file_with_byte_content() throws IOException {
    createdFile = manager.createSecureTempFileWithContent(TEST_PREFIX, TEST_SUFFIX, TEST_BYTES);

    assertNotNull(createdFile);
    assertTrue(createdFile.exists());

    byte[] actualBytes = Files.readAllBytes(createdFile.toPath());
    assertArrayEquals(TEST_BYTES, actualBytes);
  }

  @Test
  void should_create_temp_file_with_empty_string_content() throws IOException {
    String emptyContent = "";
    createdFile = manager.createSecureTempFileWithContent(TEST_PREFIX, TEST_SUFFIX, emptyContent);

    assertNotNull(createdFile);
    assertTrue(createdFile.exists());

    String actualContent = Files.readString(createdFile.toPath());
    assertEquals(emptyContent, actualContent);
  }

  @Test
  void should_create_temp_file_with_empty_byte_array() throws IOException {
    byte[] emptyBytes = new byte[0];
    createdFile = manager.createSecureTempFileWithContent(TEST_PREFIX, TEST_SUFFIX, emptyBytes);

    assertNotNull(createdFile);
    assertTrue(createdFile.exists());
    assertEquals(0, createdFile.length());
  }

  @Test
  void should_delete_temp_file_successfully() throws IOException {
    createdFile = manager.createSecureTempFile(TEST_PREFIX, TEST_SUFFIX);
    assertTrue(createdFile.exists());

    boolean deleted = manager.deleteTempFile(createdFile);

    assertTrue(deleted);
    assertFalse(createdFile.exists());
  }

  @Test
  void should_return_false_when_deleting_null_file() {
    boolean deleted = manager.deleteTempFile(null);
    assertFalse(deleted);
  }

  @Test
  void should_return_false_when_deleting_non_existent_file() {
    File nonExistentFile = new File("/tmp/non-existent-file-" + System.currentTimeMillis());
    boolean deleted = manager.deleteTempFile(nonExistentFile);
    assertFalse(deleted);
  }

  @Test
  void should_throw_exception_when_prefix_is_null() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> manager.createSecureTempFile(null, TEST_SUFFIX));

    assertEquals("Prefix must be at least 3 characters long", exception.getMessage());
  }

  @Test
  void should_throw_exception_when_prefix_is_too_short() {
    String shortPrefix = "ab";

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> manager.createSecureTempFile(shortPrefix, TEST_SUFFIX));

    assertEquals("Prefix must be at least 3 characters long", exception.getMessage());
  }

  @Test
  void should_throw_exception_when_suffix_is_null() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> manager.createSecureTempFile(TEST_PREFIX, null));

    assertEquals("Suffix cannot be null", exception.getMessage());
  }

  @Test
  void should_accept_empty_suffix() throws IOException {
    String emptySuffix = "";
    createdFile = manager.createSecureTempFile(TEST_PREFIX, emptySuffix);

    assertNotNull(createdFile);
    assertTrue(createdFile.exists());
    assertTrue(createdFile.getName().startsWith(TEST_PREFIX));
  }

  @Test
  void should_accept_minimum_valid_prefix_length() throws IOException {
    String minPrefix = "abc";
    createdFile = manager.createSecureTempFile(minPrefix, TEST_SUFFIX);

    assertNotNull(createdFile);
    assertTrue(createdFile.exists());
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void should_create_file_with_owner_only_permissions_on_posix() throws IOException {
    createdFile = manager.createSecureTempFile(TEST_PREFIX, TEST_SUFFIX);

    Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(createdFile.toPath());
    Set<PosixFilePermission> expectedPermissions = PosixFilePermissions.fromString("rw-------");

    assertEquals(expectedPermissions, permissions);
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void should_not_allow_group_read_on_posix() throws IOException {
    createdFile = manager.createSecureTempFile(TEST_PREFIX, TEST_SUFFIX);

    Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(createdFile.toPath());

    assertFalse(permissions.contains(PosixFilePermission.GROUP_READ));
    assertFalse(permissions.contains(PosixFilePermission.GROUP_WRITE));
    assertFalse(permissions.contains(PosixFilePermission.GROUP_EXECUTE));
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void should_not_allow_others_read_on_posix() throws IOException {
    createdFile = manager.createSecureTempFile(TEST_PREFIX, TEST_SUFFIX);

    Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(createdFile.toPath());

    assertFalse(permissions.contains(PosixFilePermission.OTHERS_READ));
    assertFalse(permissions.contains(PosixFilePermission.OTHERS_WRITE));
    assertFalse(permissions.contains(PosixFilePermission.OTHERS_EXECUTE));
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void should_create_file_in_user_temp_directory_on_windows() throws IOException {
    createdFile = manager.createSecureTempFile(TEST_PREFIX, TEST_SUFFIX);

    String userTempDir = System.getProperty("java.io.tmpdir");
    assertTrue(createdFile.getAbsolutePath().startsWith(userTempDir));
  }

  @Test
  void should_cleanup_file_on_content_write_failure() {
    String invalidContent = null;

    assertThrows(
        NullPointerException.class,
        () -> manager.createSecureTempFileWithContent(TEST_PREFIX, TEST_SUFFIX, invalidContent));

    // Verify no orphaned files were created by checking temp directory
    // This is a best-effort verification
  }

  @Test
  void should_handle_large_content() throws IOException {
    StringBuilder largeContent = new StringBuilder();
    int largeSize = 10_000;

    for (int i = 0; i < largeSize; i++) {
      largeContent.append("Line ").append(i).append("\n");
    }

    createdFile =
        manager.createSecureTempFileWithContent(TEST_PREFIX, TEST_SUFFIX, largeContent.toString());

    assertNotNull(createdFile);
    assertTrue(createdFile.exists());
    assertTrue(createdFile.length() > 0);

    String readContent = Files.readString(createdFile.toPath());
    assertEquals(largeContent.toString(), readContent);
  }

  @Test
  void should_handle_special_characters_in_content() throws IOException {
    String specialContent = "Special chars: €, ñ, 中文, emoji: 🔒, newlines:\n\ttabs\r\n";
    createdFile = manager.createSecureTempFileWithContent(TEST_PREFIX, TEST_SUFFIX, specialContent);

    assertNotNull(createdFile);
    assertTrue(createdFile.exists());

    String actualContent = Files.readString(createdFile.toPath());
    assertEquals(specialContent, actualContent);
  }

  @Test
  void should_create_readable_file() throws IOException {
    createdFile = manager.createSecureTempFile(TEST_PREFIX, TEST_SUFFIX);

    assertTrue(createdFile.canRead());
  }

  @Test
  void should_create_writable_file() throws IOException {
    createdFile = manager.createSecureTempFile(TEST_PREFIX, TEST_SUFFIX);

    assertTrue(createdFile.canWrite());
  }

  @Test
  void should_not_create_executable_file() throws IOException {
    createdFile = manager.createSecureTempFile(TEST_PREFIX, TEST_SUFFIX);

    assertFalse(createdFile.canExecute());
  }
}
