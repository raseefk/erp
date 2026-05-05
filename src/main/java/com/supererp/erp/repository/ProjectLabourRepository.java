package com.supererp.erp.repository;

import com.supererp.erp.entity.ProjectLabour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectLabourRepository extends JpaRepository<ProjectLabour, Long> {
    List<ProjectLabour> findByProjectIdOrderByNameAsc(Long projectId);
}
