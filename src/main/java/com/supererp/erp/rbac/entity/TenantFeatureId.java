package com.supererp.erp.rbac.entity;

import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor
public class TenantFeatureId implements Serializable {
    private UUID tenantId;
    private String featureId;
}
