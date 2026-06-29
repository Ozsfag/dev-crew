package org.blacksoil.devcrew.notification.domain.store;

import java.util.List;
import java.util.Optional;
import org.blacksoil.devcrew.notification.domain.model.ChatSessionModel;

/** Port персистентности сессий диалога Telegram-чата. */
public interface ChatSessionStore {

  /** Активный (выбранный) проект чата, если есть. */
  Optional<ChatSessionModel> findCurrent(long chatId);

  /** Сессия конкретного проекта в чате, если есть. */
  Optional<ChatSessionModel> find(long chatId, String projectName);

  /** Все сессии чата (по всем проектам). */
  List<ChatSessionModel> findByChatId(long chatId);

  /** Создаёт или обновляет сессию. */
  ChatSessionModel save(ChatSessionModel session);
}
