package com.msa.userserver.domain.feign;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.domain.dto.MessageDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "account", fallbackFactory = AccountClientFallbackFactory.class)
public interface AccountClient {

    @GetMapping("/api/stores/phones")
    ResponseEntity<ApiResponse<List<MessageDto.StorePhoneInfo>>> getStorePhones(
            @RequestHeader Map<String, Object> headers,
            @RequestParam("ids") List<Long> storeIds
    );
}
