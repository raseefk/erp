package com.supererp.erp.rbac.repository;

import com.supererp.erp.rbac.entity.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FeatureRepository extends JpaRepository<Feature, String> {
    @Query("SELECT f FROM Feature f ORDER BY f.sortOrder ASC")
    List<Feature> findAllOrdered();
}
