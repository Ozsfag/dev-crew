package org.blacksoil.devcrew.agent.domain.check;

import java.util.UUID;

/**
 * Extension point для проверок перед запуском агента. Реализации регистрируются как Spring-бины и
 * вызываются оркестратором в методе run(). Бросает DomainException если запуск запрещён.
 */
public interface PreRunCheck {

  void check(UUID projectId);
}
