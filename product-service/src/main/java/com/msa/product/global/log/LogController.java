package com.msa.product.global.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LogController {

    private static final Logger logger = LoggerFactory.getLogger(LogController.class);

    // gateway에서 별도의 권환 확인 후 전달 가능하게
    @GetMapping("/log")
    public String logTest() {
        logger.info("✅ 로그 파일 저장: INFO");
        return "로그 저장 완료";
    }
}
