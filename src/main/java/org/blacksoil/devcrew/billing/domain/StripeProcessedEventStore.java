package org.blacksoil.devcrew.billing.domain;

public interface StripeProcessedEventStore {

  boolean isProcessed(String eventId);

  void markAsProcessed(String eventId);
}
