package org.blacksoil.devcrew.billing.adapter.in.stripe;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.blacksoil.devcrew.billing.app.config.BillingProperties;
import org.blacksoil.devcrew.billing.domain.StripeWebhookPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
public class StripeWebhookAdapter {

  private final StripeWebhookPort stripeWebhookPort;
  private final BillingProperties billingProperties;

  @PostMapping("/webhook")
  public ResponseEntity<Void> handleWebhook(
      @RequestBody String payload,
      @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
    if (signature == null) {
      log.warn("Отсутствует заголовок Stripe-Signature");
      return ResponseEntity.badRequest().build();
    }
    Event event;
    try {
      event =
          Webhook.constructEvent(payload, signature, billingProperties.getStripeWebhookSecret());
    } catch (SignatureVerificationException e) {
      log.warn("Невалидная подпись Stripe webhook");
      return ResponseEntity.badRequest().build();
    }

    switch (event.getType()) {
      case "customer.subscription.created" -> {
        var subscription =
            (Subscription) event.getDataObjectDeserializer().getObject().orElseThrow();
        log.info("Stripe subscription.created: customer={}", subscription.getCustomer());
        stripeWebhookPort.handleSubscriptionCreated(subscription.getCustomer());
      }
      case "customer.subscription.deleted" -> {
        var subscription =
            (Subscription) event.getDataObjectDeserializer().getObject().orElseThrow();
        log.info("Stripe subscription.deleted: customer={}", subscription.getCustomer());
        stripeWebhookPort.handleSubscriptionDeleted(subscription.getCustomer());
      }
      default -> log.debug("Игнорируем событие Stripe: type={}", event.getType());
    }
    return ResponseEntity.ok().build();
  }
}
