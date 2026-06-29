package org.blacksoil.devcrew.notification.adapter.out.persistence.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.blacksoil.devcrew.common.IntegrationTestBase;
import org.blacksoil.devcrew.notification.domain.model.ChatSessionModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ChatSessionJpaStoreTest extends IntegrationTestBase {

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

  @Autowired private ChatSessionJpaStore store;

  @Test
  void save_and_findCurrent_roundtrip() {
    store.save(session(777L, "vpn-app", "sess-1", true));

    var found = store.findCurrent(777L);

    assertThat(found).isPresent();
    assertThat(found.get().projectName()).isEqualTo("vpn-app");
    assertThat(found.get().claudeSessionId()).isEqualTo("sess-1");
  }

  @Test
  void findCurrent_empty_when_no_active_project() {
    store.save(session(778L, "vpn-app", null, false));

    assertThat(store.findCurrent(778L)).isEmpty();
  }

  @Test
  void find_returns_session_by_chat_and_project() {
    store.save(session(779L, "dev-crew", "sess-x", false));

    var found = store.find(779L, "dev-crew");

    assertThat(found).isPresent();
    assertThat(found.get().claudeSessionId()).isEqualTo("sess-x");
  }

  @Test
  void findByChatId_returns_all_projects_of_chat() {
    store.save(session(780L, "vpn-app", null, true));
    store.save(session(780L, "dev-crew", null, false));

    assertThat(store.findByChatId(780L)).hasSize(2);
  }

  @Test
  void save_updates_existing_session_id() {
    var saved = store.save(session(781L, "vpn-app", null, true));

    store.save(
        new ChatSessionModel(saved.id(), 781L, "vpn-app", "resumed", true, saved.createdAt(), NOW));

    assertThat(store.find(781L, "vpn-app"))
        .get()
        .extracting(ChatSessionModel::claudeSessionId)
        .isEqualTo("resumed");
  }

  private static ChatSessionModel session(
      long chatId, String project, String claudeSessionId, boolean current) {
    return new ChatSessionModel(
        UUID.randomUUID(), chatId, project, claudeSessionId, current, NOW, NOW);
  }
}
