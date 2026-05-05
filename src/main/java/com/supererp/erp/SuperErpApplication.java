package com.supererp.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class SuperErpApplication {
    public static void main(String[] args) {
        SpringApplication.run(SuperErpApplication.class, args);
    }
}
