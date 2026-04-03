package org.blacksoil.devcrew.auth.adapter.out.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import org.blacksoil.devcrew.auth.adapter.out.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

  Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

  @Modifying
  @Query("UPDATE RefreshTokenEntity t SET t.revoked = true WHERE t.userId = :userId")
  void revokeByUserId(@Param("userId") UUID userId);
}
