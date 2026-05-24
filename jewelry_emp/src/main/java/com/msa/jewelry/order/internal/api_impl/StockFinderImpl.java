package com.msa.jewelry.order.internal.api_impl;

import com.msa.jewelry.order.api.StockFinder;
import com.msa.jewelry.order.api.StockView;
import com.msa.jewelry.order.internal.stock.entity.ProductSnapshot;
import com.msa.jewelry.order.internal.stock.entity.Stock;
import com.msa.jewelry.order.internal.stock.repository.StockRepository;
import com.msa.jewelry.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 다른 모듈(예: product 의 CatalogService)이 재고 정보를 조회할 때 쓰는 공개 API 구현체.
 *
 * <p>2026-05 P1 단계에서 신설. 기존에는 product 모듈이 {@code StockClient} (Feign) 으로
 * order-service 를 호출했으나, 모놀로식 통합 후에는 같은 JVM 안에서 본 빈을 호출한다.
 *
 * <p>{@link StockView} 는 record 로 정의된 불변 DTO 이며, 재고 행의 ProductSnapshot
 * 임베디드 컬럼에서 노출 가능한 정보만 추출해 반환한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockFinderImpl implements StockFinder {

    private final StockRepository stockRepository;

    @Override
    public StockView getStock(Long flowCode) {
        Stock stock = stockRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new NotFoundException("Stock not found: flowCode=" + flowCode));

        ProductSnapshot snapshot = stock.getProduct();

        return new StockView(
                stock.getFlowCode(),
                stock.getOrderStatus() != null ? stock.getOrderStatus().name() : null,
                snapshot != null ? snapshot.getProductName() : null,
                snapshot != null ? snapshot.getMaterialName() : null,
                snapshot != null ? snapshot.getColorName() : null,
                snapshot != null ? snapshot.getGoldWeight() : null,
                snapshot != null ? snapshot.getStoneWeight() : null
        );
    }

    @Override
    public Map<String, Integer> getStockCountByProductNames(List<String> productNames) {
        if (productNames == null || productNames.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object[]> rows = stockRepository.countByProductNames(productNames);
        return rows.stream()
                .collect(Collectors.toMap(
                        r -> (String) r[0],
                        r -> ((Number) r[1]).intValue()
                ));
    }
}
