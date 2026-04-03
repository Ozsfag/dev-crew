package org.blacksoil.devcrew.auth.adapter.out.persistence.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.blacksoil.devcrew.auth.domain.UserModel;
import org.blacksoil.devcrew.auth.domain.UserRole;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("tc")
@Transactional
class UserJpaStoreTest {

  @Autowired private UserJpaStore userJpaStore;

  @Test
  void save_and_findById_roundtrip() {
    var user = userModel("test@test.com", UserRole.ARCHITECT);

    var saved = userJpaStore.save(user);
    var found = userJpaStore.findById(saved.id());

    assertThat(found).isPresent();
    assertThat(found.get().email()).isEqualTo("test@test.com");
    assertThat(found.get().role()).isEqualTo(UserRole.ARCHITECT);
  }

  @Test
  void findByEmail_returns_user_when_exists() {
    userJpaStore.save(userModel("find@test.com", UserRole.VIEWER));

    var found = userJpaStore.findByEmail("find@test.com");

    assertThat(found).isPresent();
    assertThat(found.get().email()).isEqualTo("find@test.com");
  }

  @Test
  void findByEmail_returns_empty_when_missing() {
    var found = userJpaStore.findByEmail("nobody@test.com");
    assertThat(found).isEmpty();
  }

  @Test
  void existsAny_returns_false_when_no_users() {
    assertThat(userJpaStore.existsAny()).isFalse();
  }

  @Test
  void existsAny_returns_true_after_save() {
    userJpaStore.save(userModel("any@test.com", UserRole.ARCHITECT));
    assertThat(userJpaStore.existsAny()).isTrue();
  }

  private UserModel userModel(String email, UserRole role) {
    return new UserModel(
        UUID.randomUUID(), null, email, "hash", role, Instant.now(), Instant.now());
  }
}
