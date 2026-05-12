package com.supererp.erp.repository;

import com.supererp.erp.entity.MaterialSiteTransaction;
import com.supererp.erp.enums.MaterialSiteTransactionType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialSiteTransactionRepository extends JpaRepository<MaterialSiteTransaction, Long> {
    
    @EntityGraph(attributePaths = {"inventoryItem"})
    List<MaterialSiteTransaction> findByProjectIdOrderByTransactionDateDesc(Long projectId);
    
    @EntityGraph(attributePaths = {"inventoryItem"})
    List<MaterialSiteTransaction> findByProjectIdAndInventoryItemIdOrderByTransactionDateDesc(Long projectId, Long inventoryItemId);
    
    @EntityGraph(attributePaths = {"inventoryItem"})
    List<MaterialSiteTransaction> findByTransactionTypeOrderByTransactionDateDesc(MaterialSiteTransactionType transactionType);
}
