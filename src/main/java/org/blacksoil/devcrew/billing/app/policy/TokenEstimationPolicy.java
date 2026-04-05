package org.blacksoil.devcrew.billing.app.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.billing.app.config.BillingProperties;
import org.springframework.stereotype.Component;

/**
 * Оценивает количество токенов по длине текста и рассчитывает стоимость запроса. Точная токенизация
 * требует tiktoken/антропик-токенизатора; оценка по символам достаточна для биллинга MVP.
 */
@Component
@RequiredArgsConstructor
public class TokenEstimationPolicy {

  private final BillingProperties properties;

  /** Оценивает количество токенов в тексте. Минимум 1 токен для непустого текста. */
  public int estimateTokensApproximate(String text) {
    if (text == null || text.isEmpty()) {
      return 0;
    }
    return Math.max(1, text.length() / properties.getCharsPerToken());
  }

  /** Рассчитывает стоимость запроса в USD по количеству входящих и исходящих токенов. */
  public BigDecimal calculateCost(int promptTokens, int completionTokens) {
    var inputCost = properties.getCostPerInputToken().multiply(BigDecimal.valueOf(promptTokens));
    var outputCost =
        properties.getCostPerOutputToken().multiply(BigDecimal.valueOf(completionTokens));
    return inputCost.add(outputCost).setScale(8, RoundingMode.HALF_UP);
  }
}
