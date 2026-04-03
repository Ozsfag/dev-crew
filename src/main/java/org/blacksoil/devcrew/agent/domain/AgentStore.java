package org.blacksoil.devcrew.agent.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port-интерфейс хранилища агентов. Реализуется в adapter/out. */
public interface AgentStore {

  AgentModel save(AgentModel agent);

  Optional<AgentModel> findById(UUID id);

  Optional<AgentModel> findByRole(AgentRole role);

  List<AgentModel> findAll();
}
