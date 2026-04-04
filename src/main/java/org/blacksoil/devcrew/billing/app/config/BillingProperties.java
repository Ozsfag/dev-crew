package org.blacksoil.devcrew.billing.app.config;

import java.math.BigDecimal;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "devcrew.billing")
public class BillingProperties {

  /** Стоимость одного входящего токена в USD (claude-sonnet-4-6: $3 / MTok). */
  private BigDecimal costPerInputToken = new BigDecimal("0.000003");

  /** Стоимость одного исходящего токена в USD (claude-sonnet-4-6: $15 / MTok). */
  private BigDecimal costPerOutputToken = new BigDecimal("0.000015");

  /** Среднее количество символов на один токен для оценки. */
  private int charsPerToken = 4;

  /** Лимит задач в месяц для плана FREE. */
  private int freePlanTaskLimit = 50;

  /** Секрет для проверки подписи Stripe webhook (whsec_...). */
  private String stripeWebhookSecret = "";
}
