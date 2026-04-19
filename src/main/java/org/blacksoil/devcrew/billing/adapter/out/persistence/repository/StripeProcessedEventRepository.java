package org.blacksoil.devcrew.billing.adapter.out.persistence.repository;

import org.blacksoil.devcrew.billing.adapter.out.persistence.entity.StripeProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeProcessedEventRepository
    extends JpaRepository<StripeProcessedEventEntity, String> {}
