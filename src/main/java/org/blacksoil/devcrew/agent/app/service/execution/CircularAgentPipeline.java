package org.blacksoil.devcrew.agent.app.service.execution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.blacksoil.devcrew.agent.app.config.AgentProperties;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.springframework.stereotype.Service;

/**
 * Циклический пайплайн агентов: BackendDev → QA → CodeReview → loop при необходимости. Максимальное
 * число итераций ограничено agentProperties.maxIterations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CircularAgentPipeline {

  private final AgentDispatcher agentDispatcher;
  private final AgentProperties agentProperties;

  /**
   * Выполняет задачу через циклический пайплайн агентов.
   *
   * @param task описание задачи
   * @return итоговый результат после успешного прохода всего пайплайна
   */
  public String execute(String task) {
    log.info("CircularAgentPipeline запущен");

    // Фаза 1: BackendDev пишет код
    var codeResult = agentDispatcher.dispatch(AgentRole.BACKEND_DEV, task);
    var maxIterations = agentProperties.getMaxIterations();

    for (int iteration = 0; iteration < maxIterations; iteration++) {
      log.debug("CircularAgentPipeline итерация {}/{}", iteration + 1, maxIterations);

      // Фаза 2: QA пишет и запускает тесты
      var qaResult = agentDispatcher.dispatch(AgentRole.QA, buildQaPrompt(task, codeResult));

      var pipeline = agentProperties.getPipeline();
      if (qaResult.contains(pipeline.getBuildSuccessfulMarker())) {
        log.info("Тесты прошли на итерации {}", iteration + 1);

        // Фаза 3: CodeReview
        var reviewResult = agentDispatcher.dispatch(AgentRole.CODE_REVIEWER, codeResult);
        if (!reviewResult.contains(pipeline.getRequestChangesMarker())) {
          log.info("CodeReview одобрил результат");
          return reviewResult;
        }
        log.info("CodeReview запросил изменения, возвращаемся к BackendDev");
        codeResult =
            agentDispatcher.dispatch(
                AgentRole.BACKEND_DEV, buildReviewFixPrompt(task, reviewResult));
      } else {
        log.info("Тесты не прошли на итерации {}, возвращаемся к BackendDev", iteration + 1);
        codeResult =
            agentDispatcher.dispatch(AgentRole.BACKEND_DEV, buildFixPrompt(task, qaResult));
      }
    }

    log.warn(
        "CircularAgentPipeline достиг максимума итераций ({}), возвращаем последний результат",
        maxIterations);
    return codeResult;
  }

  private String buildQaPrompt(String originalTask, String codeResult) {
    return "Original task: "
        + originalTask
        + "\n\nCode written by BackendDev:\n"
        + codeResult
        + "\n\nWrite tests and run them. Report BUILD SUCCESSFUL if all tests pass.";
  }

  private String buildFixPrompt(String originalTask, String qaResult) {
    return "Original task: "
        + originalTask
        + "\n\nTest results (FAILED):\n"
        + qaResult
        + "\n\nFix the code to make all tests pass.";
  }

  private String buildReviewFixPrompt(String originalTask, String reviewResult) {
    return "Original task: "
        + originalTask
        + "\n\nCode review feedback (REQUEST_CHANGES):\n"
        + reviewResult
        + "\n\nApply the requested changes.";
  }
}
