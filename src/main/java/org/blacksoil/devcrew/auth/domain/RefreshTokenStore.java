package org.blacksoil.devcrew.auth.domain;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenStore {

  RefreshTokenModel save(RefreshTokenModel token);

  Optional<RefreshTokenModel> findByTokenHash(String tokenHash);

  void revokeByUserId(UUID userId);
}
