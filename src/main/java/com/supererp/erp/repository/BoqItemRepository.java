package com.supererp.erp.repository;

import com.supererp.erp.entity.BoqItem;
import com.supererp.erp.enums.BoqItemStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoqItemRepository extends JpaRepository<BoqItem, Long> {
    
    @EntityGraph(attributePaths = {"inventoryItem"})
    List<BoqItem> findByBoqIdOrderByIdAsc(Long boqId);
    
    @EntityGraph(attributePaths = {"inventoryItem"})
    List<BoqItem> findByProjectIdAndStatusOrderByIdAsc(Long projectId, BoqItemStatus status);

    @EntityGraph(attributePaths = {"inventoryItem", "boq"})
    List<BoqItem> findByProjectIdOrderByDescriptionAsc(Long projectId);
}
