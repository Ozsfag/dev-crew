package org.blacksoil.devcrew.auth.domain;

import java.util.Optional;
import java.util.UUID;

public interface UserStore {

    UserModel save(UserModel user);

    Optional<UserModel> findByEmail(String email);

    Optional<UserModel> findById(UUID id);

    boolean existsAny();
}
