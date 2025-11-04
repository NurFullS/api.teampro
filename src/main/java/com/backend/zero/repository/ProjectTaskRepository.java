package com.backend.zero.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.backend.zero.model.ProjectTask;

public interface ProjectTaskRepository extends JpaRepository<ProjectTask, Long> {
    List<ProjectTask> findByProjectId(Long projectId);
}
