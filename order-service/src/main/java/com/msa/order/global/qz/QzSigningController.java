package com.msa.order.global.qz;

import com.msa.common.global.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
public class QzSigningController {

    private final QzSigningService qzSigningService;

    public QzSigningController(QzSigningService qzSigningService) {
        this.qzSigningService = qzSigningService;
    }

    @PostMapping("/api/qz/sign")
    public ResponseEntity<ApiResponse<Map<String, String>>> signMessage(
            @RequestBody SignRequest request) throws Exception {
        String signature = qzSigningService.sign(request.getDataToSign());
        return ResponseEntity.ok(ApiResponse.success(Collections.singletonMap("signature", signature)));
    }

    public static class SignRequest {
        private String dataToSign;
        public String getDataToSign() {
            return dataToSign;
        }
        public void setDataToSign(String dataToSign) { this.dataToSign = dataToSign; }
    }

}
