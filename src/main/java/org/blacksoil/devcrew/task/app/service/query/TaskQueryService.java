package org.blacksoil.devcrew.task.app.service.query;

import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.common.exception.NotFoundException;
import org.blacksoil.devcrew.task.domain.TaskModel;
import org.blacksoil.devcrew.task.domain.TaskStatus;
import org.blacksoil.devcrew.task.domain.TaskStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TaskQueryService {

    private final TaskStore taskStore;

    public TaskModel getById(UUID id) {
        return taskStore.findById(id)
            .orElseThrow(() -> new NotFoundException("Task", id));
    }

    public List<TaskModel> getByStatus(TaskStatus status) {
        return taskStore.findByStatus(status);
    }

    public List<TaskModel> getSubtasks(UUID parentTaskId) {
        return taskStore.findByParentTaskId(parentTaskId);
    }
}
