package org.blacksoil.devcrew.auth.app.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.blacksoil.devcrew.common.exception.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthRateLimitServiceTest {

  private AuthRateLimitService service;

  @BeforeEach
  void setUp() {
    service = new AuthRateLimitService();
  }

  @Test
  void checkLogin_allows_up_to_10_attempts() {
    for (int i = 0; i < 10; i++) {
      assertThatCode(() -> service.checkLoginAttempt("user@test.com")).doesNotThrowAnyException();
    }
  }

  @Test
  void checkLogin_throws_after_10_attempts() {
    for (int i = 0; i < 10; i++) {
      service.checkLoginAttempt("user@test.com");
    }
    assertThatThrownBy(() -> service.checkLoginAttempt("user@test.com"))
        .isInstanceOf(TooManyRequestsException.class);
  }

  @Test
  void checkLogin_tracks_buckets_per_email() {
    for (int i = 0; i < 10; i++) {
      service.checkLoginAttempt("alice@test.com");
    }
    // другой email — свой бакет, попытки не исчерпаны
    assertThatCode(() -> service.checkLoginAttempt("bob@test.com")).doesNotThrowAnyException();
  }

  @Test
  void checkRegister_allows_up_to_5_attempts() {
    for (int i = 0; i < 5; i++) {
      assertThatCode(() -> service.checkRegisterAttempt("user@test.com"))
          .doesNotThrowAnyException();
    }
  }

  @Test
  void checkRegister_throws_after_5_attempts() {
    for (int i = 0; i < 5; i++) {
      service.checkRegisterAttempt("user@test.com");
    }
    assertThatThrownBy(() -> service.checkRegisterAttempt("user@test.com"))
        .isInstanceOf(TooManyRequestsException.class);
  }

  @Test
  void checkRegister_tracks_buckets_independently_from_login() {
    for (int i = 0; i < 5; i++) {
      service.checkRegisterAttempt("user@test.com");
    }
    // register исчерпан, но login — свой бакет
    assertThatCode(() -> service.checkLoginAttempt("user@test.com")).doesNotThrowAnyException();
  }
}
