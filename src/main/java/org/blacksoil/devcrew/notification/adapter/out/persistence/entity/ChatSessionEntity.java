package org.blacksoil.devcrew.notification.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "telegram_chat_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionEntity {

  @Id private UUID id;

  @Column(name = "chat_id", nullable = false)
  private long chatId;

  @Column(name = "project_name", nullable = false)
  private String projectName;

  @Column(name = "claude_session_id")
  private String claudeSessionId;

  @Column(name = "is_current", nullable = false)
  private boolean current;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
