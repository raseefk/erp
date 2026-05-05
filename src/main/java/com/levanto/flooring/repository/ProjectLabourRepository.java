package com.levanto.flooring.repository;

import com.levanto.flooring.entity.ProjectLabour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectLabourRepository extends JpaRepository<ProjectLabour, Long> {
    List<ProjectLabour> findByProjectIdOrderByNameAsc(Long projectId);
}
