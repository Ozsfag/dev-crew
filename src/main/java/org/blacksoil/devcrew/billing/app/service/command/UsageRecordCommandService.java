package org.blacksoil.devcrew.billing.app.service.command;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.billing.app.policy.TokenEstimationPolicy;
import org.blacksoil.devcrew.billing.domain.UsageRecordModel;
import org.blacksoil.devcrew.billing.domain.UsageRecordStore;
import org.blacksoil.devcrew.common.TimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UsageRecordCommandService {

  private final UsageRecordStore usageRecordStore;
  private final TokenEstimationPolicy tokenEstimationPolicy;
  private final TimeProvider timeProvider;

  /**
   * Записывает потребление токенов и стоимость за выполнение задачи агентом.
   *
   * @param prompt текст задания (входящие токены)
   * @param result ответ агента (исходящие токены)
   */
  public UsageRecordModel record(
      UUID taskId, UUID projectId, UUID orgId, AgentRole role, String prompt, String result) {
    int promptTokens = tokenEstimationPolicy.estimateTokens(prompt);
    int completionTokens = tokenEstimationPolicy.estimateTokens(result);
    var cost = tokenEstimationPolicy.calculateCost(promptTokens, completionTokens);
    var usageRecord =
        new UsageRecordModel(
            UUID.randomUUID(),
            taskId,
            projectId,
            orgId,
            role,
            promptTokens,
            completionTokens,
            cost,
            timeProvider.now());
    return usageRecordStore.save(usageRecord);
  }
}
