package org.blacksoil.devcrew.auth.adapter.out.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import org.blacksoil.devcrew.auth.adapter.out.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

  Optional<UserEntity> findByEmail(String email);
}
