package com.supererp.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

/**
 * A specific location within a warehouse — zone, aisle, rack, shelf, bin.
 * Hierarchical: Zone > Aisle > Rack > Shelf > Bin
 */
@Entity
@Table(name = "warehouse_locations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id","warehouse_id","barcode"}))
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class WarehouseLocation extends TenantAwareEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(nullable = false, length = 50)
    private String barcode; // unique scannable code for this location

    @Column(length = 50)
    private String zone;    // e.g. "A"

    @Column(length = 50)
    private String aisle;   // e.g. "A1"

    @Column(length = 50)
    private String rack;    // e.g. "R3"

    @Column(length = 50)
    private String shelf;   // e.g. "S2"

    @Column(length = 50)
    private String bin;     // e.g. "B01"

    /** Human-readable full address: Zone-Aisle-Rack-Shelf-Bin */
    @Column(name = "full_address", length = 200)
    private String fullAddress;

    @Column(name = "max_capacity_units")
    private Integer maxCapacityUnits;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() {
        createdAt = LocalDateTime.now();
        // Build full address from parts
        StringBuilder sb = new StringBuilder();
        if (zone  != null && !zone.isBlank())  sb.append(zone);
        if (aisle != null && !aisle.isBlank()) sb.append(sb.length() > 0 ? "-" : "").append(aisle);
        if (rack  != null && !rack.isBlank())  sb.append(sb.length() > 0 ? "-" : "").append(rack);
        if (shelf != null && !shelf.isBlank()) sb.append(sb.length() > 0 ? "-" : "").append(shelf);
        if (bin   != null && !bin.isBlank())   sb.append(sb.length() > 0 ? "-" : "").append(bin);
        if (fullAddress == null || fullAddress.isBlank()) fullAddress = sb.toString();
    }
}
