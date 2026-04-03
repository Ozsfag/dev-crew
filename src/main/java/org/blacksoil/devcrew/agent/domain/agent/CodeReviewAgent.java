package org.blacksoil.devcrew.agent.domain.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Domain-порт CodeReview агента. Анализирует git diff, проверяет по чеклисту и возвращает
 * review-отчёт. Реализуется через LangChain4j AiServices в bootstrap-слое.
 */
@SystemMessage(fromResource = "prompts/code-review.md")
public interface CodeReviewAgent {

  String execute(@UserMessage String task);
}
