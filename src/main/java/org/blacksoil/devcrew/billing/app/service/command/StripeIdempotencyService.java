package org.blacksoil.devcrew.billing.app.service.command;

import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.billing.domain.StripeProcessedEventStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StripeIdempotencyService {

  private final StripeProcessedEventStore store;

  @Transactional
  public boolean tryMarkProcessed(String eventId) {
    if (store.isProcessed(eventId)) {
      return false;
    }
    store.markAsProcessed(eventId);
    return true;
  }
}
