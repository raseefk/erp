package com.levanto.flooring.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.company")
@Data
public class CompanyProperties {
    private String name        = "Levanto Flooring";
    private String tagline     = "Premium Flooring Solutions";
    private String address     = "Your Shop Address, City, State — PIN";
    private String phone       = "+91 XXXXX XXXXX";
    private String email       = "info@levantoflooring.com";
    private String website     = "www.levantoflooring.com";
    private String gstNumber   = "YOUR_GSTIN";
    private int    cgstRate    = 9;
    private int    sgstRate    = 9;
}
