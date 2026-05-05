package com.supererp.erp.repository;
import com.supererp.erp.entity.InventoryItem;
import com.supererp.erp.enums.ItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    long countByTenantId(UUID tenantId);
    List<InventoryItem> findByActiveTrueOrderByNameAsc();
    List<InventoryItem> findByItemTypeAndActiveTrueOrderByNameAsc(ItemType type);
    @Query("SELECT i FROM InventoryItem i WHERE i.active=true AND (LOWER(i.name) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(i.hsnSacCode) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<InventoryItem> searchActive(@Param("q") String q);
    @Query("SELECT i FROM InventoryItem i WHERE (LOWER(i.name) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(i.hsnSacCode) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<InventoryItem> searchAll(@Param("q") String q, Pageable p);
    List<InventoryItem> findByItemTypeAndActiveTrueAndStockQuantityLessThan(ItemType type, int threshold);
}

