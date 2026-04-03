package org.blacksoil.devcrew.agent.domain.store;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.model.AgentModel;

/** Port-интерфейс хранилища агентов. Реализуется в adapter/out. */
public interface AgentStore {

  AgentModel save(AgentModel agent);

  Optional<AgentModel> findById(UUID id);

  Optional<AgentModel> findByRole(AgentRole role);

  List<AgentModel> findAll();
}
