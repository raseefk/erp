package com.supererp.erp.repository;
import com.supererp.erp.entity.JobCard;
import com.supererp.erp.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface JobCardRepository extends JpaRepository<JobCard, Long> {

    @EntityGraph(attributePaths = {"project", "assignedEngineer"})
    Optional<JobCard> findById(Long id);

    @EntityGraph(attributePaths = {"project", "assignedEngineer", "dailyLogs"})
    List<JobCard> findByProjectOrderByCreatedAtDesc(Project project);

    @EntityGraph(attributePaths = {"project", "assignedEngineer"})
    List<JobCard> findByAssignedEngineerIdOrderByCreatedAtDesc(Long engineerId);
}
