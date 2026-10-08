package com.connecthub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

// No UserDetailsService: authentication is JWT/OAuth2 only, so skip Boot's generated in-memory user.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
@EnableJpaAuditing
@EnableScheduling
public class ConnectHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConnectHubApplication.class, args);
    }
}
