package org.blacksoil.devcrew.billing.adapter.out.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stripe_processed_event")
@AllArgsConstructor
@NoArgsConstructor
public class StripeProcessedEventEntity {

  @Id private String eventId;

  private Instant processedAt;
}
