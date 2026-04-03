package org.blacksoil.devcrew.agent.adapter.out.persistence.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.AgentStatus;
import org.blacksoil.devcrew.agent.domain.model.AgentModel;
import org.blacksoil.devcrew.common.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AgentJpaStoreTest extends IntegrationTestBase {

  @Autowired private AgentJpaStore agentJpaStore;

  @Test
  void save_and_findById_roundtrip() {
    var agent = agentModel(AgentRole.BACKEND_DEV);

    var saved = agentJpaStore.save(agent);
    var found = agentJpaStore.findById(saved.id());

    assertThat(found).isPresent();
    assertThat(found.get().role()).isEqualTo(AgentRole.BACKEND_DEV);
    assertThat(found.get().status()).isEqualTo(AgentStatus.IDLE);
    assertThat(found.get().systemPrompt()).isEqualTo(agent.systemPrompt());
  }

  @Test
  void findByRole_returns_correct_agent() {
    agentJpaStore.save(agentModel(AgentRole.QA));
    agentJpaStore.save(agentModel(AgentRole.DEVOPS));

    var found = agentJpaStore.findByRole(AgentRole.QA);

    assertThat(found).isPresent();
    assertThat(found.get().role()).isEqualTo(AgentRole.QA);
  }

  @Test
  void findByRole_returns_empty_when_missing() {
    var found = agentJpaStore.findByRole(AgentRole.PM);

    assertThat(found).isEmpty();
  }

  @Test
  void findAll_returns_all_saved_agents() {
    agentJpaStore.save(agentModel(AgentRole.BACKEND_DEV));
    agentJpaStore.save(agentModel(AgentRole.QA));

    var all = agentJpaStore.findAll();

    assertThat(all).hasSize(2);
  }

  @Test
  void save_updates_existing_agent() {
    var saved = agentJpaStore.save(agentModel(AgentRole.BACKEND_DEV));
    var updated =
        new AgentModel(
            saved.id(),
            saved.role(),
            AgentStatus.RUNNING,
            saved.systemPrompt(),
            saved.createdAt(),
            Instant.now());

    var result = agentJpaStore.save(updated);

    assertThat(result.status()).isEqualTo(AgentStatus.RUNNING);
  }

  private AgentModel agentModel(AgentRole role) {
    return new AgentModel(
        UUID.randomUUID(),
        role,
        AgentStatus.IDLE,
        "system prompt for " + role,
        Instant.now(),
        Instant.now());
  }
}
