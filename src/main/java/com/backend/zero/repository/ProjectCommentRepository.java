package com.backend.zero.repository;

import com.backend.zero.model.ProjectComment;
import com.backend.zero.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectCommentRepository extends JpaRepository<ProjectComment, Long> {

    List<ProjectComment> findAllByProjectOrderByCreatedAtAsc(Project project);

    List<ProjectComment> findAllByProjectIdOrderByCreatedAtAsc(Long projectId);

    List<ProjectComment> findAllByProjectId(Long projectId);
}
