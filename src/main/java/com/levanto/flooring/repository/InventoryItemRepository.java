package com.levanto.flooring.repository;
import com.levanto.flooring.entity.InventoryItem;
import com.levanto.flooring.enums.ItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    List<InventoryItem> findByActiveTrueOrderByNameAsc();
    List<InventoryItem> findByItemTypeAndActiveTrueOrderByNameAsc(ItemType type);
    @Query("SELECT i FROM InventoryItem i WHERE i.active=true AND (LOWER(i.name) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(i.hsnSacCode) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<InventoryItem> searchActive(@Param("q") String q);
    @Query("SELECT i FROM InventoryItem i WHERE (LOWER(i.name) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(i.hsnSacCode) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<InventoryItem> searchAll(@Param("q") String q, Pageable p);
    List<InventoryItem> findByItemTypeAndActiveTrueAndStockQuantityLessThan(ItemType type, int threshold);
}
