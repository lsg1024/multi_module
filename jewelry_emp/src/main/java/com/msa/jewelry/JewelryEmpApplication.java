package com.msa.jewelry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@EnableRetry
@EntityScan(basePackages = "com.msa.jewelry")
@ComponentScan(basePackages = {
        "com.msa.jewelry",
        "com.msa.common"
})
public class JewelryEmpApplication {

    public static void main(String[] args) {
        // 애플리케이션 전역 타임존을 KST(Asia/Seoul) 로 고정.
        // - LocalDateTime.now() 호출이 컨테이너 JVM(보통 UTC) 에 영향받지 않고 항상 KST 시각을 반환
        // - Jackson, hibernate.jdbc.time_zone 도 application.yml 에서 KST 로 통일됨
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(JewelryEmpApplication.class, args);
    }
}
