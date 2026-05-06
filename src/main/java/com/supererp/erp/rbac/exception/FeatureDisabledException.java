package com.supererp.erp.rbac.exception;

import lombok.Getter;

@Getter
public class FeatureDisabledException extends RuntimeException {
    private final String featureName;

    public FeatureDisabledException(String featureName) {
        super("This feature is not enabled for your subscription: " + featureName);
        this.featureName = featureName;
    }
}
