package com.capstore;

import com.capstore.config.DataSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertFalse;

class DataSeederActivationTest {
    @Test
    void seedingIsDisabledUnlessExplicitlyEnabled() {
        new ApplicationContextRunner()
                .withUserConfiguration(DataSeeder.class)
                .run(context -> assertFalse(context.containsBean("dataSeeder")));
    }

    @Test
    void productionProfileNeverRegistersSeederEvenIfExplicitlyEnabled() {
        new ApplicationContextRunner()
                .withUserConfiguration(DataSeeder.class)
                .withPropertyValues("spring.profiles.active=prod", "app.seed.enabled=true")
                .run(context -> assertFalse(context.containsBean("dataSeeder")));
    }
}
