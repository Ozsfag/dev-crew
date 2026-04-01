package org.blacksoil.devcrew.auth.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.auth.domain.RefreshTokenModel;
import org.blacksoil.devcrew.auth.domain.RefreshTokenStore;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RefreshTokenJpaStore implements RefreshTokenStore {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenPersistenceMapper mapper;

    @Override
    @Transactional
    public RefreshTokenModel save(RefreshTokenModel token) {
        return mapper.toModel(refreshTokenRepository.save(mapper.toEntity(token)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshTokenModel> findByTokenHash(String tokenHash) {
        return refreshTokenRepository.findByTokenHash(tokenHash).map(mapper::toModel);
    }

    @Override
    @Transactional
    public void revokeByUserId(UUID userId) {
        refreshTokenRepository.revokeByUserId(userId);
    }
}
