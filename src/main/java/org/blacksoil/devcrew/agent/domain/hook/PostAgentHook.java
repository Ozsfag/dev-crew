package org.blacksoil.devcrew.agent.domain.hook;

import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentRole;

/**
 * Extension point: вызывается после завершения агента. Реализуется другими bounded contexts
 * (например, notification). Agent-модуль не знает о реализациях — Spring связывает через
 * List<PostAgentHook>.
 */
public interface PostAgentHook {

  void onAgentCompleted(UUID taskId, UUID projectId, AgentRole role, String result);
}
