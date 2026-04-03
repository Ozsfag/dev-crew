package org.blacksoil.devcrew.task.adapter.out.persistence.store;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.task.adapter.out.persistence.mapper.TaskPersistenceMapper;
import org.blacksoil.devcrew.task.adapter.out.persistence.repository.TaskRepository;
import org.blacksoil.devcrew.task.domain.TaskModel;
import org.blacksoil.devcrew.task.domain.TaskStatus;
import org.blacksoil.devcrew.task.domain.TaskStore;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class TaskJpaStore implements TaskStore {

  private final TaskRepository taskRepository;
  private final TaskPersistenceMapper mapper;

  @Override
  @Transactional
  public TaskModel save(TaskModel task) {
    return mapper.toModel(taskRepository.save(mapper.toEntity(task)));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<TaskModel> findById(UUID id) {
    return taskRepository.findById(id).map(mapper::toModel);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TaskModel> findByStatus(TaskStatus status) {
    return taskRepository.findByStatus(status).stream().map(mapper::toModel).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<TaskModel> findByParentTaskId(UUID parentTaskId) {
    return taskRepository.findByParentTaskId(parentTaskId).stream().map(mapper::toModel).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<TaskModel> findByProjectId(UUID projectId) {
    return taskRepository.findByProjectId(projectId).stream().map(mapper::toModel).toList();
  }
}
