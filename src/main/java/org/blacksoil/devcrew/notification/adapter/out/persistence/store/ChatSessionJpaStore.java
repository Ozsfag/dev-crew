package org.blacksoil.devcrew.notification.adapter.out.persistence.store;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.notification.adapter.out.persistence.mapper.ChatSessionPersistenceMapper;
import org.blacksoil.devcrew.notification.adapter.out.persistence.repository.ChatSessionRepository;
import org.blacksoil.devcrew.notification.domain.model.ChatSessionModel;
import org.blacksoil.devcrew.notification.domain.store.ChatSessionStore;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class ChatSessionJpaStore implements ChatSessionStore {

  private final ChatSessionRepository repository;
  private final ChatSessionPersistenceMapper mapper;

  @Override
  @Transactional(readOnly = true)
  public Optional<ChatSessionModel> findCurrent(long chatId) {
    return repository.findByChatIdAndCurrentTrue(chatId).map(mapper::toModel);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ChatSessionModel> find(long chatId, String projectName) {
    return repository.findByChatIdAndProjectName(chatId, projectName).map(mapper::toModel);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ChatSessionModel> findByChatId(long chatId) {
    return repository.findByChatId(chatId).stream().map(mapper::toModel).toList();
  }

  @Override
  @Transactional
  public ChatSessionModel save(ChatSessionModel session) {
    var entity = mapper.toEntity(session);
    return mapper.toModel(repository.save(entity));
  }
}
