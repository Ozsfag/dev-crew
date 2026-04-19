package org.blacksoil.devcrew.billing.app.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.blacksoil.devcrew.billing.domain.StripeProcessedEventStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StripeIdempotencyServiceTest {

  @Mock private StripeProcessedEventStore store;
  @InjectMocks private StripeIdempotencyService service;

  @Test
  void tryMarkProcessed_returns_true_and_saves_when_event_is_new() {
    when(store.isProcessed("evt_123")).thenReturn(false);

    assertThat(service.tryMarkProcessed("evt_123")).isTrue();
    verify(store).markAsProcessed("evt_123");
  }

  @Test
  void tryMarkProcessed_returns_false_and_skips_save_when_already_processed() {
    when(store.isProcessed("evt_dup")).thenReturn(true);

    assertThat(service.tryMarkProcessed("evt_dup")).isFalse();
    verify(store, org.mockito.Mockito.never()).markAsProcessed("evt_dup");
  }
}
