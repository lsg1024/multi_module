package com.msa.jewelry.order.api;

import java.util.List;
import java.util.Map;

/**
 * 다른 모듈이 재고 정보를 조회할 때 사용하는 공개 API.
 *
 * <p>기존 ProductFeignClient.getStockInfo / OrderFeignClient.getStockCountByProductNames
 * 등의 동기 등가물.
 */
public interface StockFinder {

    /**
     * 흐름 코드(flowCode)로 단건 재고 view 조회.
     */
    StockView getStock(Long flowCode);

    /**
     * 상품명 목록 → 활성 재고 수량 집계 (productName → count).
     *
     * <p>기존 StockClient.getStockCountByProductNames 의 동기 등가물.
     */
    Map<String, Integer> getStockCountByProductNames(List<String> productNames);
}
