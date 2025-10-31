package com.backend.zero.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.zero.model.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findAllByOwnerId(Long ownerId);
}
