package org.blacksoil.devcrew.billing.app.service.command;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.billing.app.policy.TokenEstimationPolicy;
import org.blacksoil.devcrew.billing.domain.UsageRecordModel;
import org.blacksoil.devcrew.billing.domain.UsageRecordStore;
import org.blacksoil.devcrew.common.TimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UsageRecordCommandService {

  private final UsageRecordStore usageRecordStore;
  private final TokenEstimationPolicy tokenEstimationPolicy;
  private final TimeProvider timeProvider;

  /**
   * Записывает потребление токенов и стоимость за выполнение задачи агентом. Идемпотентен:
   * повторный вызов с тем же taskId пропускается.
   *
   * @param prompt текст задания (входящие токены)
   * @param result ответ агента (исходящие токены)
   */
  public UsageRecordModel record(
      UUID taskId, UUID projectId, UUID orgId, AgentRole role, String prompt, String result) {
    if (usageRecordStore.existsByTaskId(taskId)) {
      log.debug("Запись об использовании для задачи {} уже существует — пропуск", taskId);
      return null;
    }
    int promptTokens = tokenEstimationPolicy.estimateTokensApproximate(prompt);
    int completionTokens = tokenEstimationPolicy.estimateTokensApproximate(result);
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
