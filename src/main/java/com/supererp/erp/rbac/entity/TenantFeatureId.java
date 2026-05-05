package com.supererp.erp.rbac.entity;

import java.io.Serializable;
import java.util.UUID;

public class TenantFeatureId implements Serializable {
    private UUID tenantId;
    private String featureId;
}
