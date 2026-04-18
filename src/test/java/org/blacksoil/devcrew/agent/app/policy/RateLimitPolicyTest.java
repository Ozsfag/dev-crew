package org.blacksoil.devcrew.agent.app.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.blacksoil.devcrew.agent.app.config.RateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RateLimitPolicyTest {

  private RateLimitPolicy policy;
  private RateLimitProperties properties;

  @BeforeEach
  void setUp() {
    properties = new RateLimitProperties();
    policy = new RateLimitPolicy(properties);
  }

  @Test
  void isRateLimit_returns_true_for_429_in_message() {
    var ex = new RuntimeException("HTTP 429 Too Many Requests");
    assertThat(policy.isRateLimit(ex)).isTrue();
  }

  @Test
  void isRateLimit_returns_true_for_rate_limit_phrase() {
    var ex = new RuntimeException("rate limit exceeded");
    assertThat(policy.isRateLimit(ex)).isTrue();
  }

  @Test
  void isRateLimit_returns_true_for_rate_limit_error_type() {
    var ex = new RuntimeException("{\"type\":\"rate_limit_error\",\"message\":\"limit hit\"}");
    assertThat(policy.isRateLimit(ex)).isTrue();
  }

  @Test
  void isRateLimit_returns_true_for_overloaded_in_message() {
    var ex = new RuntimeException("Anthropic is temporarily overloaded");
    assertThat(policy.isRateLimit(ex)).isTrue();
  }

  @Test
  void isRateLimit_returns_true_for_too_many_requests_in_message() {
    var ex = new RuntimeException("too many requests");
    assertThat(policy.isRateLimit(ex)).isTrue();
  }

  @Test
  void isRateLimit_returns_true_when_cause_contains_rate_limit() {
    var cause = new RuntimeException("HTTP 429");
    var ex = new RuntimeException("LLM call failed", cause);
    assertThat(policy.isRateLimit(ex)).isTrue();
  }

  @Test
  void isRateLimit_returns_false_for_generic_build_error() {
    var ex = new RuntimeException("Build failed: compilation error");
    assertThat(policy.isRateLimit(ex)).isFalse();
  }

  @Test
  void isRateLimit_returns_false_for_npe() {
    var ex = new NullPointerException();
    assertThat(policy.isRateLimit(ex)).isFalse();
  }

  @Test
  void retryAt_returns_now_plus_default_retry_delay() {
    var now = Instant.parse("2026-01-01T10:00:00Z");
    var retryAt = policy.retryAt(now);
    assertThat(retryAt).isEqualTo(now.plus(Duration.ofSeconds(60)));
  }

  @Test
  void retryAt_respects_custom_retry_delay() {
    properties.setRetryDelay(Duration.ofSeconds(120));
    var now = Instant.parse("2026-01-01T10:00:00Z");
    var retryAt = policy.retryAt(now);
    assertThat(retryAt).isEqualTo(now.plus(Duration.ofSeconds(120)));
  }
}
