package org.blacksoil.devcrew.auth.domain.store;

import java.util.Optional;
import java.util.UUID;
import org.blacksoil.devcrew.auth.domain.model.UserModel;

public interface UserStore {

  UserModel save(UserModel user);

  Optional<UserModel> findByEmail(String email);

  Optional<UserModel> findById(UUID id);

  boolean existsAny();
}
