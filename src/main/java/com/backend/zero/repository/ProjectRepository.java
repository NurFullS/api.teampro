package com.backend.zero.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.backend.zero.model.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findAllByOwnerId(Long ownerId);
    List<Project> findByOwnerIdAndNameContainingIgnoreCase(Long ownerId, String name);
    Optional<Project> findByNameAndAccessCode(String name, String accessCode);

    @Query("SELECT p FROM Project p LEFT JOIN p.developers d WHERE p.owner.id = :userId OR d.id = :userId")
    List<Project> findAllByUserId(@Param("userId") Long userId);

}
