package com.msa.jewelry.local.qz.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import com.msa.common.global.util.AuthorityUserRoleUtil;
import com.msa.jewelry.global.exception.NotAuthorityException;
import com.msa.jewelry.local.qz.service.QzSigningService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
public class QzSigningController {

    private final QzSigningService qzSigningService;
    private final AuthorityUserRoleUtil authorityUserRoleUtil;

    public QzSigningController(QzSigningService qzSigningService, AuthorityUserRoleUtil authorityUserRoleUtil) {
        this.qzSigningService = qzSigningService;
        this.authorityUserRoleUtil = authorityUserRoleUtil;
    }

    @PostMapping("/api/qz/sign")
    public ResponseEntity<ApiResponse<Map<String, String>>> signMessage(
            @AccessToken String accessToken,
            @Valid @RequestBody SignRequest request) throws Exception {
        if (!authorityUserRoleUtil.verification(accessToken)) {
            throw new NotAuthorityException("권한이 없습니다.");
        }
        String signature = qzSigningService.sign(request.getDataToSign());
        return ResponseEntity.ok(ApiResponse.success(Collections.singletonMap("signature", signature)));
    }

    public static class SignRequest {
        @NotBlank(message = "서명 대상이 비어 있습니다.")
        @Size(max = 4096, message = "서명 대상이 너무 깁니다.")
        private String dataToSign;
        public String getDataToSign() {
            return dataToSign;
        }
        public void setDataToSign(String dataToSign) { this.dataToSign = dataToSign; }
    }

}
