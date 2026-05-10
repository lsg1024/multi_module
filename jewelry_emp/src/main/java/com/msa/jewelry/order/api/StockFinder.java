package com.msa.jewelry.order.api;

/**
 * 다른 모듈이 재고 정보를 조회할 때 사용하는 공개 API.
 *
 * <p>기존 ProductFeignClient.getStockInfo 등의 동기 등가물.
 */
public interface StockFinder {

    StockView getStock(Long flowCode);
}
