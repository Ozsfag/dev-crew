package org.blacksoil.devcrew.auth.domain.store;

import java.util.Optional;
import java.util.UUID;
import org.blacksoil.devcrew.auth.domain.model.RefreshTokenModel;

public interface RefreshTokenStore {

  RefreshTokenModel save(RefreshTokenModel token);

  Optional<RefreshTokenModel> findByTokenHash(String tokenHash);

  void revokeByUserId(UUID userId);
}
