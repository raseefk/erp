package com.levanto.flooring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class LevantoFlooringApplication {
    public static void main(String[] args) {
        SpringApplication.run(LevantoFlooringApplication.class, args);
    }
}
