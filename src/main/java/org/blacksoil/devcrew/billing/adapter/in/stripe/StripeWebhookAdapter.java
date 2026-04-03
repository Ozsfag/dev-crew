package org.blacksoil.devcrew.billing.adapter.in.stripe;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stub-адаптер для приёма webhook-событий от Stripe. Принимает и логирует события; в production
 * необходимо: 1) проверять подпись через Stripe SDK, 2) обновлять план организации при
 * customer.subscription.* событиях.
 */
@Slf4j
@RestController
@RequestMapping("/api/stripe")
public class StripeWebhookAdapter {

  @PostMapping("/webhook")
  public ResponseEntity<Void> handleWebhook(
      @RequestBody String payload,
      @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
    log.info(
        "Получено событие Stripe: payload_length={}, signature={}",
        payload.length(),
        signature != null ? "присутствует" : "отсутствует");
    // TODO: Stripe SDK — Webhook.constructEvent(payload, signature, webhookSecret)
    // TODO: customer.subscription.created → апгрейд плана до PRO
    // TODO: customer.subscription.deleted → даунгрейд до FREE
    return ResponseEntity.ok().build();
  }
}
