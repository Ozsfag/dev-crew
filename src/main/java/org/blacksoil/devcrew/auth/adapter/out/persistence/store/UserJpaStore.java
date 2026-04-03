package org.blacksoil.devcrew.auth.adapter.out.persistence.store;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.auth.adapter.out.persistence.mapper.UserPersistenceMapper;
import org.blacksoil.devcrew.auth.adapter.out.persistence.repository.UserRepository;
import org.blacksoil.devcrew.auth.domain.model.UserModel;
import org.blacksoil.devcrew.auth.domain.store.UserStore;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class UserJpaStore implements UserStore {

  private final UserRepository userRepository;
  private final UserPersistenceMapper mapper;

  @Override
  @Transactional
  public UserModel save(UserModel user) {
    return mapper.toModel(userRepository.save(mapper.toEntity(user)));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UserModel> findByEmail(String email) {
    return userRepository.findByEmail(email).map(mapper::toModel);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UserModel> findById(UUID id) {
    return userRepository.findById(id).map(mapper::toModel);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsAny() {
    return userRepository.count() > 0;
  }
}
