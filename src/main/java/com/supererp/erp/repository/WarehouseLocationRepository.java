package com.supererp.erp.repository;

import com.supererp.erp.entity.WarehouseLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocation, Long> {

    List<WarehouseLocation> findByWarehouseIdAndActiveTrueOrderByFullAddressAsc(Long warehouseId);

    List<WarehouseLocation> findByActiveTrueOrderByFullAddressAsc();

    Optional<WarehouseLocation> findByBarcodeAndWarehouseId(String barcode, Long warehouseId);

    Optional<WarehouseLocation> findByBarcode(String barcode);

    @Query("SELECT l FROM WarehouseLocation l JOIN FETCH l.warehouse WHERE l.active = true ORDER BY l.warehouse.name ASC, l.fullAddress ASC")
    List<WarehouseLocation> findAllActiveWithWarehouse();

    @Query("SELECT l FROM WarehouseLocation l JOIN FETCH l.warehouse WHERE l.warehouse.id = :warehouseId AND l.active = true ORDER BY l.fullAddress ASC")
    List<WarehouseLocation> findByWarehouseWithWarehouse(@Param("warehouseId") Long warehouseId);
}
