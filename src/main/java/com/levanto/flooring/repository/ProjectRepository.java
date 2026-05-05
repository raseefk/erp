package com.levanto.flooring.repository;
import com.levanto.flooring.entity.Project;
import com.levanto.flooring.enums.ProjectStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    
    @EntityGraph(attributePaths = {"jobCards"})
    Optional<Project> findById(Long id);

    @EntityGraph(attributePaths = {"jobCards"})
    List<Project> findByStatusOrderByCreatedAtDesc(ProjectStatus status);

    @Query("SELECT p.id as id, p.name as name, p.clientName as clientName, p.location as location, " +
           "p.totalContractValue as totalContractValue, p.startDate as startDate, p.endDate as endDate, " +
           "p.status as status, SIZE(p.jobCards) as jobCardCount FROM Project p")
    Page<com.levanto.flooring.projection.ProjectSummary> findAllSummaries(Pageable p);

    @Query("SELECT p.id as id, p.name as name, p.clientName as clientName, p.location as location, " +
           "p.totalContractValue as totalContractValue, p.startDate as startDate, p.endDate as endDate, " +
           "p.status as status, SIZE(p.jobCards) as jobCardCount FROM Project p " +
           "WHERE (:q IS NULL OR :q = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(p.clientName) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<com.levanto.flooring.projection.ProjectSummary> searchSummaries(@Param("q") String q, Pageable p);

    long countByStatus(ProjectStatus status);
}
