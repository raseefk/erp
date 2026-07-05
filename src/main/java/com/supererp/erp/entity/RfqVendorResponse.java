package com.supererp.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Vendor's response to an RFQ — contains their quoted prices.
 */
@Entity
@Table(name = "rfq_vendor_responses")
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class RfqVendorResponse extends TenantAwareEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id", nullable = false)
    private RequestForQuotation rfq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "response_date")
    private LocalDate responseDate;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "total_quoted_amount", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalQuotedAmount = BigDecimal.ZERO;

    @Column(name = "delivery_days")
    private Integer deliveryDays;

    @Column(name = "payment_terms", length = 200)
    private String paymentTerms;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "is_awarded")
    @Builder.Default
    private Boolean awarded = false;

    @OneToMany(mappedBy = "vendorResponse", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RfqVendorResponseItem> responseItems = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
