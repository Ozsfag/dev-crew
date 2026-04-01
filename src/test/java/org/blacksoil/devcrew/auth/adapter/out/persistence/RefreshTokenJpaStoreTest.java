package org.blacksoil.devcrew.auth.adapter.out.persistence;

import org.blacksoil.devcrew.auth.domain.RefreshTokenModel;
import org.blacksoil.devcrew.auth.domain.UserModel;
import org.blacksoil.devcrew.auth.domain.UserRole;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("tc")
@Transactional
class RefreshTokenJpaStoreTest {

    @Autowired
    private UserJpaStore userJpaStore;

    @Autowired
    private RefreshTokenJpaStore refreshTokenJpaStore;

    @Test
    void save_and_findByTokenHash_roundtrip() {
        var user = userJpaStore.save(userModel());
        var token = refreshTokenModel(user.id(), "hash-abc", false);

        refreshTokenJpaStore.save(token);
        var found = refreshTokenJpaStore.findByTokenHash("hash-abc");

        assertThat(found).isPresent();
        assertThat(found.get().userId()).isEqualTo(user.id());
        assertThat(found.get().revoked()).isFalse();
    }

    @Test
    void findByTokenHash_returns_empty_when_missing() {
        var found = refreshTokenJpaStore.findByTokenHash("unknown-hash");
        assertThat(found).isEmpty();
    }

    @Test
    void revokeByUserId_marks_all_tokens_revoked() {
        var user = userJpaStore.save(userModel());
        refreshTokenJpaStore.save(refreshTokenModel(user.id(), "hash-1", false));
        refreshTokenJpaStore.save(refreshTokenModel(user.id(), "hash-2", false));

        refreshTokenJpaStore.revokeByUserId(user.id());

        assertThat(refreshTokenJpaStore.findByTokenHash("hash-1"))
            .isPresent().get()
            .extracting(RefreshTokenModel::revoked)
            .isEqualTo(true);
        assertThat(refreshTokenJpaStore.findByTokenHash("hash-2"))
            .isPresent().get()
            .extracting(RefreshTokenModel::revoked)
            .isEqualTo(true);
    }

    private UserModel userModel() {
        return new UserModel(UUID.randomUUID(), UUID.randomUUID() + "@test.com",
            "hash", UserRole.VIEWER, Instant.now(), Instant.now());
    }

    private RefreshTokenModel refreshTokenModel(UUID userId, String hash, boolean revoked) {
        return new RefreshTokenModel(UUID.randomUUID(), userId, hash,
            Instant.now().plusSeconds(3600), revoked, Instant.now());
    }
}
