package com.msa.order.local.domain.stock.service;

import com.msa.order.global.kafka.dto.KafkaStockRequest;
import com.msa.order.local.domain.order.dto.FactoryDto;
import com.msa.order.local.domain.order.dto.StoreDto;
import com.msa.order.local.domain.order.entity.StatusHistory;
import com.msa.order.local.domain.order.entity.order_enum.OrderStatus;
import com.msa.order.local.domain.order.external_client.*;
import com.msa.order.local.domain.order.external_client.dto.ProductDetailDto;
import com.msa.order.local.domain.order.repository.StatusHistoryRepository;
import com.msa.order.local.domain.stock.entity.domain.ProductSnapshot;
import com.msa.order.local.domain.stock.entity.domain.Stock;
import com.msa.order.local.domain.stock.repository.StockRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;
import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND_STONE;

@Slf4j
@Service
@Transactional
public class KafkaStockService {

    private final StoneClient stoneClient;
    private final StoreClient storeClient;
    private final FactoryClient factoryClient;
    private final ProductClient productClient;
    private final MaterialClient materialClient;
    private final ColorClient colorClient;
    private final ClassificationClient classificationClient;
    private final StockRepository stockRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    public KafkaStockService(StoneClient stoneClient, StoreClient storeClient, FactoryClient factoryClient, ProductClient productClient, MaterialClient materialClient, ColorClient colorClient, ClassificationClient classificationClient, StockRepository stockRepository, StatusHistoryRepository statusHistoryRepository) {
        this.stoneClient = stoneClient;
        this.storeClient = storeClient;
        this.factoryClient = factoryClient;
        this.productClient = productClient;
        this.materialClient = materialClient;
        this.colorClient = colorClient;
        this.classificationClient = classificationClient;
        this.stockRepository = stockRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    public void saveStockDetail(KafkaStockRequest stockRequest) {
        final String tenantId = stockRequest.getTenantId();

        Stock stock = stockRepository.findByFlowCode(stockRequest.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        if (stock.getOrderStatus() != OrderStatus.NORMAL) {
            return;
        }

        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(stock.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        StatusHistory statusHistory;

        try {
            StoreDto.Response storeInfo;
            String factoryName;
            String materialName;
            String classificationName;
            String colorName;

            if (stockRequest.getStoreId() != null) {
                storeInfo = storeClient.getStoreInfo(tenantId, stockRequest.getStoreId());
            } else {
                storeInfo = storeClient.getStoreInfo(tenantId, 1L);
            }

            if (stockRequest.getFactoryId() != null) {
                factoryName = factoryClient.getFactoryInfo(tenantId, stockRequest.getFactoryId()).getFactoryName();
            } else {
                factoryName = factoryClient.getFactoryInfo(tenantId, 1L).getFactoryName();
            }

            if (stockRequest.getMaterialId() != null) {
                materialName = materialClient.getMaterialInfo(tenantId, stockRequest.getMaterialId());
            } else {
                materialName = materialClient.getMaterialInfo(tenantId, 1L);
            }

            if (stockRequest.getClassificationId() != null) {
                classificationName = classificationClient.getClassificationInfo(tenantId, stockRequest.getClassificationId());
            } else {
                classificationName = classificationClient.getClassificationInfo(tenantId, 1L);
            }

            if (stockRequest.getColorId() != null) {
                colorName = colorClient.getColorInfo(tenantId, stockRequest.getColorId());
            } else {
                colorName = colorClient.getColorInfo(tenantId, 1L);
            }

            ProductDetailDto productInfo = productClient.getProductInfo(tenantId, stockRequest.getProductId(), storeInfo.getGrade());

            ProductSnapshot product = stock.getProduct();
            product.updateProduct(
                    productInfo.getProductName(),
                    productInfo.getLaborCost(),
                    materialName,
                    classificationName,
                    colorName
            );

            List<Long> stoneIds = stockRequest.getStoneIds();
            for (Long stoneId : stoneIds) {
                Boolean existStoneId = stoneClient.getExistStoneId(tenantId, stoneId);
                if (!existStoneId) {
                    throw new IllegalStateException(NOT_FOUND_STONE);
                }
            }

            stock.updateStore(new StoreDto.Response(stockRequest.getStoreId(), storeInfo.getStoreName()));
            stock.updateFactory(new FactoryDto.Response(stockRequest.getFactoryId(), factoryName));

            statusHistory = StatusHistory.phaseChange(
                    stock.getFlowCode(),
                    lastHistory.getSourceType(),
                    lastHistory.getPhase(),
                    StatusHistory.BusinessPhase.STOCK,
                    stockRequest.getNickname()
            );

            statusHistoryRepository.save(statusHistory);

        } catch (Exception e) {
            log.error("Async failed. orderId={}, err={}", stockRequest.getFlowCode(), e.getMessage(), e);

            statusHistory = StatusHistory.phaseChange(
                    stock.getFlowCode(),
                    lastHistory.getSourceType(),
                    lastHistory.getPhase(),
                    StatusHistory.BusinessPhase.STOCK_FAIL,
                    stockRequest.getNickname()
            );

            statusHistoryRepository.save(statusHistory);
        }
    }
}
