package org.blacksoil.devcrew.notification.adapter.out.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import org.blacksoil.devcrew.notification.adapter.out.telegram.dto.TelegramFile;
import org.junit.jupiter.api.Test;

class TelegramFileValidatorTest {

  @Test
  void isAllowed_returns_true_for_txt_within_size_limit() {
    assertThat(TelegramFileValidator.isAllowed(file("documents/readme.txt", 1024L))).isTrue();
  }

  @Test
  void isAllowed_returns_true_for_md_file() {
    assertThat(TelegramFileValidator.isAllowed(file("notes.md", 500L))).isTrue();
  }

  @Test
  void isAllowed_returns_true_for_json_file() {
    assertThat(TelegramFileValidator.isAllowed(file("data.json", 2048L))).isTrue();
  }

  @Test
  void isAllowed_returns_false_for_exe_extension() {
    assertThat(TelegramFileValidator.isAllowed(file("malware.exe", 100L))).isFalse();
  }

  @Test
  void isAllowed_returns_false_for_zip_extension() {
    assertThat(TelegramFileValidator.isAllowed(file("archive.zip", 100L))).isFalse();
  }

  @Test
  void isAllowed_returns_false_when_file_exceeds_20mb() {
    long oversized = 20 * 1024 * 1024L + 1;
    assertThat(TelegramFileValidator.isAllowed(file("large.txt", oversized))).isFalse();
  }

  @Test
  void isAllowed_returns_true_when_file_exactly_20mb() {
    long limit = 20 * 1024 * 1024L;
    assertThat(TelegramFileValidator.isAllowed(file("big.txt", limit))).isTrue();
  }

  @Test
  void isAllowed_returns_false_when_fileSize_is_null_and_extension_disallowed() {
    assertThat(TelegramFileValidator.isAllowed(file("photo.jpg", null))).isFalse();
  }

  @Test
  void isAllowed_returns_true_when_fileSize_is_null_and_extension_allowed() {
    assertThat(TelegramFileValidator.isAllowed(file("notes.txt", null))).isTrue();
  }

  @Test
  void isAllowed_returns_false_when_filePath_has_no_extension() {
    assertThat(TelegramFileValidator.isAllowed(file("noextension", 100L))).isFalse();
  }

  private static TelegramFile file(String filePath, Long fileSize) {
    return new TelegramFile(filePath, fileSize);
  }
}
