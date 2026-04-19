package org.blacksoil.devcrew.billing.adapter.out.persistence.store;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.billing.adapter.out.persistence.entity.StripeProcessedEventEntity;
import org.blacksoil.devcrew.billing.adapter.out.persistence.repository.StripeProcessedEventRepository;
import org.blacksoil.devcrew.billing.domain.StripeProcessedEventStore;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class StripeProcessedEventJpaStore implements StripeProcessedEventStore {

  private final StripeProcessedEventRepository repository;

  @Override
  @Transactional(readOnly = true)
  public boolean isProcessed(String eventId) {
    return repository.existsById(eventId);
  }

  @Override
  @Transactional
  public void markAsProcessed(String eventId) {
    repository.save(new StripeProcessedEventEntity(eventId, Instant.now()));
  }
}
