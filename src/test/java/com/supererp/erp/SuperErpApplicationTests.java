package com.supererp.erp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = SuperErpApplication.class)
@ActiveProfiles("test")
class SuperErpApplicationTests {

    @Test
    void contextLoads() {
    }
}
