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

    @Query("SELECT DISTINCT f FROM Feature f LEFT JOIN FETCH f.menus ORDER BY f.sortOrder ASC")
    List<Feature> findAllWithMenus();

    @Query("SELECT DISTINCT f FROM Feature f " +
           "LEFT JOIN FETCH f.menus m " +
           "LEFT JOIN FETCH m.permissions " +
           "ORDER BY f.sortOrder ASC")
    List<Feature> findAllFullHierarchy();
}
