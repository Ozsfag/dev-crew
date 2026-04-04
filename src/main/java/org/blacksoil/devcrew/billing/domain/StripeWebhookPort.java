package org.blacksoil.devcrew.billing.domain;

/** Port для обработки событий Stripe webhook, связанных с подписками. */
public interface StripeWebhookPort {

  void handleSubscriptionCreated(String stripeCustomerId);

  void handleSubscriptionDeleted(String stripeCustomerId);
}
