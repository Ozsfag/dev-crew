package org.blacksoil.devcrew.billing.app.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.blacksoil.devcrew.billing.app.config.BillingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TokenEstimationPolicyTest {

  private TokenEstimationPolicy policy;

  @BeforeEach
  void setUp() {
    policy = new TokenEstimationPolicy(new BillingProperties());
  }

  @Test
  void estimateTokens_returns_zero_for_null() {
    assertThat(policy.estimateTokens(null)).isZero();
  }

  @Test
  void estimateTokens_returns_zero_for_empty_string() {
    assertThat(policy.estimateTokens("")).isZero();
  }

  @Test
  void estimateTokens_returns_at_least_one_for_short_text() {
    assertThat(policy.estimateTokens("Hi")).isEqualTo(1);
  }

  @Test
  void estimateTokens_divides_length_by_chars_per_token() {
    // default charsPerToken = 4, text of 40 chars → 10 tokens
    var text = "a".repeat(40);
    assertThat(policy.estimateTokens(text)).isEqualTo(10);
  }

  @Test
  void calculateCost_zero_tokens_returns_zero() {
    assertThat(policy.calculateCost(0, 0)).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void calculateCost_uses_input_and_output_rates() {
    // 1_000_000 input tokens at $3/MTok = $3.00
    // 1_000_000 output tokens at $15/MTok = $15.00 → total $18.00
    var cost = policy.calculateCost(1_000_000, 1_000_000);
    assertThat(cost).isEqualByComparingTo(new BigDecimal("18.00000000"));
  }

  @Test
  void calculateCost_scales_to_8_decimal_places() {
    var cost = policy.calculateCost(1, 1);
    assertThat(cost.scale()).isEqualTo(8);
  }
}
