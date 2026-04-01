package org.blacksoil.devcrew.agent.adapter.out.persistence;

import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgentRepository extends JpaRepository<AgentEntity, UUID> {

    Optional<AgentEntity> findByRole(AgentRole role);
}
