package org.blacksoil.devcrew.agent.adapter.out.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import org.blacksoil.devcrew.agent.adapter.out.persistence.entity.AgentEntity;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRepository extends JpaRepository<AgentEntity, UUID> {

  Optional<AgentEntity> findByRole(AgentRole role);
}
