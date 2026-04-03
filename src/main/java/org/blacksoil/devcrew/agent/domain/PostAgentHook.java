package org.blacksoil.devcrew.agent.domain;

import java.util.UUID;

/**
 * Extension point: вызывается после завершения агента. Реализуется другими bounded contexts
 * (например, notification). Agent-модуль не знает о реализациях — Spring связывает через
 * List<PostAgentHook>.
 */
public interface PostAgentHook {

  void onAgentCompleted(UUID taskId, AgentRole role, String result);
}
