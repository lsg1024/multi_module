package com.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EntityScan(basePackages = {
        "com.account.global.domain.entity",
        "com.account.domain.store.entity"
})
@EnableJpaAuditing
@EnableDiscoveryClient
@SpringBootApplication(exclude = FlywayAutoConfiguration.class, scanBasePackages = {
        "com.account", "com.msacommon"
})
public class AccountServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(com.account.AccountServerApplication.class, args);
    }

}