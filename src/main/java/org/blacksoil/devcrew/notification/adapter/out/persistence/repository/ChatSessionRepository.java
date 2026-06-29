package org.blacksoil.devcrew.notification.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.blacksoil.devcrew.notification.adapter.out.persistence.entity.ChatSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, UUID> {

  Optional<ChatSessionEntity> findByChatIdAndCurrentTrue(long chatId);

  Optional<ChatSessionEntity> findByChatIdAndProjectName(long chatId, String projectName);

  List<ChatSessionEntity> findByChatId(long chatId);
}
