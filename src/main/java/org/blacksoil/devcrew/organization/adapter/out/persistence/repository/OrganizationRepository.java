package org.blacksoil.devcrew.organization.adapter.out.persistence.repository;

import java.util.UUID;
import org.blacksoil.devcrew.organization.adapter.out.persistence.entity.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<OrganizationEntity, UUID> {}
