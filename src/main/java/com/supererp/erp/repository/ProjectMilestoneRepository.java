package com.supererp.erp.repository;

import com.supererp.erp.entity.ProjectMilestone;
import com.supererp.erp.enums.ProjectMilestoneStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, Long> {
    
    @EntityGraph(attributePaths = {"project"})
    Optional<ProjectMilestone> findById(Long id);

    @EntityGraph(attributePaths = {"project"})
    List<ProjectMilestone> findByProjectIdOrderByDueDateAsc(Long projectId);
    
    @EntityGraph(attributePaths = {"project"})
    List<ProjectMilestone> findByStatusOrderByDueDateAsc(ProjectMilestoneStatus status);

    @EntityGraph(attributePaths = {"project"})
    @Query("SELECT m FROM ProjectMilestone m WHERE (:q IS NULL OR :q = '' OR LOWER(m.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(m.project.name) LIKE LOWER(CONCAT('%', :q, '%'))) ORDER BY m.dueDate ASC, m.id DESC")
    Page<ProjectMilestone> search(@Param("q") String q, Pageable pageable);
}
