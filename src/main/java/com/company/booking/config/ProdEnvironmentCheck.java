package com.company.booking.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * Stops a prod startup with a clear message when a required environment variable is missing
 * (specs/04_deployment/01-deployment.spec.adoc). Without it an unresolved
 * {@code ${MAIN_DATASOURCE_URL}} reaches the datasource as is and fails with
 * "'url' must start with jdbc", which does not name the variable.
 */
@Profile("prod")
@Configuration
public class ProdEnvironmentCheck {

    static final List<String> REQUIRED_VARIABLES = List.of(
            "MAIN_DATASOURCE_URL", "MAIN_DATASOURCE_USERNAME", "MAIN_DATASOURCE_PASSWORD", "FILE_STORAGE_DIR");

    // A BeanFactoryPostProcessor runs before any bean, including the datasource, is created
    @Bean
    static BeanFactoryPostProcessor requiredEnvironmentVariablesCheck(Environment environment) {
        return beanFactory -> {
            List<String> missing = REQUIRED_VARIABLES.stream()
                    .filter(name -> environment.getProperty(name, "").isBlank())
                    .toList();
            if (!missing.isEmpty()) {
                throw new IllegalStateException(
                        "Profile 'prod' requires environment variables that are not set: " + String.join(", ", missing));
            }
        };
    }
}
