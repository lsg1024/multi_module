package com.msa.jewelry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@EnableRetry
@EnableFeignClients(basePackages = "com.msa.jewelry")
@ComponentScan(basePackages = {
        "com.msa.jewelry",
        "com.msa.common"
})
public class JewelryEmpApplication {

    public static void main(String[] args) {
        SpringApplication.run(JewelryEmpApplication.class, args);
    }
}
