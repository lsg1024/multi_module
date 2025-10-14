package com.msa.order.local.stock.service;

import com.msa.order.global.kafka.dto.KafkaStockRequest;
import com.msa.order.local.order.dto.FactoryDto;
import com.msa.order.local.order.dto.StoreDto;
import com.msa.order.local.order.entity.StatusHistory;
import com.msa.order.local.order.entity.order_enum.BusinessPhase;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.order.external_client.*;
import com.msa.order.local.order.external_client.dto.AssistantStoneDto;
import com.msa.order.local.order.external_client.dto.ProductDetailDto;
import com.msa.order.local.order.repository.StatusHistoryRepository;
import com.msa.order.local.stock.entity.ProductSnapshot;
import com.msa.order.local.stock.entity.Stock;
import com.msa.order.local.stock.repository.StockRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;

@Slf4j
@Service
@Transactional
public class KafkaStockService {

    private final StoreClient storeClient;
    private final FactoryClient factoryClient;
    private final ProductClient productClient;
    private final MaterialClient materialClient;
    private final ColorClient colorClient;
    private final SetTypeClient setTypeClient;
    private final ClassificationClient classificationClient;
    private final AssistantStoneClient assistantStoneClient;
    private final StockRepository stockRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    public KafkaStockService(StoreClient storeClient, FactoryClient factoryClient, ProductClient productClient, MaterialClient materialClient, ColorClient colorClient, SetTypeClient setTypeClient, ClassificationClient classificationClient, AssistantStoneClient assistantStoneClient, StockRepository stockRepository, StatusHistoryRepository statusHistoryRepository) {
        this.storeClient = storeClient;
        this.factoryClient = factoryClient;
        this.productClient = productClient;
        this.materialClient = materialClient;
        this.colorClient = colorClient;
        this.setTypeClient = setTypeClient;
        this.classificationClient = classificationClient;
        this.assistantStoneClient = assistantStoneClient;
        this.stockRepository = stockRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    @Transactional
    public void saveStock(KafkaStockRequest stockDto) {
        final String tenantId = stockDto.getTenantId();

        Stock stock = stockRepository.findByFlowCode(stockDto.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        if (stock.getOrderStatus() != OrderStatus.NORMAL) {
            return;
        }

        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(stock.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        StatusHistory statusHistory;

        try {
            StoreDto.Response storeInfo;
            if (stockDto.getStoreId() != null && !stockDto.getStoreId().equals(stock.getStoreId())) {
                storeInfo = storeClient.getStoreInfo(tenantId, stockDto.getStoreId());
                if (!storeInfo.getStoreName().equals(stock.getStoreName())) {
                    stock.updateStore(StoreDto.Response.builder()
                            .storeId(storeInfo.getStoreId())
                            .storeName(storeInfo.getStoreName()).build());
                }
            } else {
                storeInfo = StoreDto.Response.builder()
                        .storeId(stock.getStoreId())
                        .storeName(stock.getStoreName())
                        .grade(stock.getStoreGrade())
                        .build();
            }

            if (stockDto.getFactoryId() != null && !stockDto.getFactoryId().equals(stock.getFactoryId())) {
                FactoryDto.Response latestFactoryInfo = factoryClient.getFactoryInfo(tenantId, stockDto.getFactoryId());
                if (!latestFactoryInfo.getFactoryName().equals(stock.getFactoryName())) {
                    stock.updateFactory(new FactoryDto.Response(latestFactoryInfo.getFactoryId(), latestFactoryInfo.getFactoryName(), latestFactoryInfo.getFactoryHarry()));
                }
            }

            ProductSnapshot product = stock.getProduct();

            String latestMaterialName = product.getMaterialName();
            if (stockDto.getMaterialId() != null && !stockDto.getMaterialId().equals(product.getMaterialId())) {
                latestMaterialName = materialClient.getMaterialInfo(tenantId, stockDto.getMaterialId());
            }

            String latestClassificationName = product.getClassificationName();
            if (stockDto.getClassificationId() != null && !stockDto.getClassificationId().equals(product.getClassificationId())) {
                latestClassificationName = classificationClient.getClassificationInfo(tenantId, stockDto.getClassificationId());
            }

            String latestColorName = product.getColorName();
            if (stockDto.getColorId() != null && !stockDto.getColorId().equals(product.getColorId())) {
                latestColorName = colorClient.getColorInfo(tenantId, stockDto.getColorId());
            }

            String latestSetTypeName = product.getSetTypeName();
            if (stockDto.getSetTypeId() != null && !stockDto.getSetTypeId().equals(product.getSetTypeId())) {
                latestSetTypeName = setTypeClient.getSetTypeName(tenantId, stockDto.getSetTypeId());
            }

            AssistantStoneDto.Response latestAssistantStoneInfo;
            if (stockDto.getAssistantStoneId() != null && !stockDto.getAssistantStoneId().equals(product.getAssistantStoneId())) {
                latestAssistantStoneInfo = assistantStoneClient.getAssistantStoneInfo(tenantId, stockDto.getAssistantStoneId());
            } else {
                latestAssistantStoneInfo = new AssistantStoneDto.Response(product.getAssistantStoneId(), product.getAssistantStoneName(), "");
            }

            ProductDetailDto latestProductInfo = productClient.getProductInfo(tenantId, stockDto.getProductId(), storeInfo.getGrade());

            if (!Objects.equals(latestProductInfo.getProductName(), product.getProductName()) ||
                    !Objects.equals(latestProductInfo.getLaborCost(), product.getProductLaborCost()) ||
                    !Objects.equals(latestMaterialName, product.getMaterialName()) ||
                    !Objects.equals(latestClassificationName, product.getClassificationName()) ||
                    !Objects.equals(latestColorName, product.getColorName()) ||
                    !Objects.equals(latestSetTypeName, product.getSetTypeName()) ||
                    !Objects.equals(latestAssistantStoneInfo.getAssistantStoneName(), product.getAssistantStoneName())) {

                product.updateProduct(
                        latestProductInfo.getProductName(),
                        latestProductInfo.getLaborCost(),
                        latestMaterialName,
                        latestClassificationName,
                        latestColorName,
                        latestSetTypeName,
                        stockDto.isAssistantStone(),
                        latestAssistantStoneInfo.getAssistantStoneId(),
                        latestAssistantStoneInfo.getAssistantStoneName(),
                        stockDto.getAssistantStoneCreateAt()
                );
            }

            stock.updateOrderStatus(OrderStatus.STOCK);

            statusHistory = StatusHistory.phaseChange(
                    stock.getFlowCode(),
                    lastHistory.getSourceType(),
                    lastHistory.getPhase(),
                    BusinessPhase.STOCK,
                    stockDto.getNickname()
            );

            statusHistoryRepository.save(statusHistory);

        } catch (Exception e) {
            log.error("Async failed. orderId={}, err={}", stockDto.getFlowCode(), e.getMessage(), e);

            statusHistory = StatusHistory.phaseChange(
                    stock.getFlowCode(),
                    lastHistory.getSourceType(),
                    lastHistory.getPhase(),
                    BusinessPhase.STOCK_FAIL,
                    stockDto.getNickname()
            );

            statusHistoryRepository.save(statusHistory);
        }
    }
}

//    public void saveStock(KafkaStockRequest stockDto) {
//        final String tenantId = stockDto.getTenantId();
//
//        Stock stock = stockRepository.findByFlowCode(stockDto.getFlowCode())
//                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
//
//        if (stock.getOrderStatus() != OrderStatus.NORMAL) {
//            return;
//        }
//
//        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(stock.getFlowCode())
//                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
//
//        StatusHistory statusHistory;
//
//        try {
//            StoreDto.Response storeInfo;
//            FactoryDto.Response factoryInfo;
//            String materialName;
//            String classificationName;
//            String colorName;
//            String setTypeName;
//            AssistantStoneDto.Response assistantStoneInfo;
//
//            if (stockDto.getStoreId() != null) {
//                storeInfo = storeClient.getStoreInfo(tenantId, stockDto.getStoreId());
//            } else {
//                storeInfo = storeClient.getStoreInfo(tenantId, 1L);
//            }
//
//            if (stockDto.getFactoryId() != null) {
//                factoryInfo = factoryClient.getFactoryInfo(tenantId, stockDto.getFactoryId());
//            } else {
//                factoryInfo = factoryClient.getFactoryInfo(tenantId, 1L);
//            }
//
//            if (stockDto.getMaterialId() != null) {
//                materialName = materialClient.getMaterialInfo(tenantId, stockDto.getMaterialId());
//            } else {
//                materialName = materialClient.getMaterialInfo(tenantId, 1L);
//            }
//
//            if (stockDto.getClassificationId() != null) {
//                classificationName = classificationClient.getClassificationInfo(tenantId, stockDto.getClassificationId());
//            } else {
//                classificationName = classificationClient.getClassificationInfo(tenantId, 1L);
//            }
//
//            if (stockDto.getColorId() != null) {
//                colorName = colorClient.getColorInfo(tenantId, stockDto.getColorId());
//            } else {
//                colorName = colorClient.getColorInfo(tenantId, 1L);
//            }
//
//            if (stockDto.getSetTypeId() != null) {
//                setTypeName = setTypeClient.getSetTypeName(tenantId, stockDto.getSetTypeId());
//            } else {
//                setTypeName = setTypeClient.getSetTypeName(tenantId, 1L);
//            }
//
//            if (stockDto.getAssistantStoneId() != null) {
//                assistantStoneInfo = assistantStoneClient.getAssistantStoneInfo(tenantId, stockDto.getAssistantStoneId());
//            } else {
//                assistantStoneInfo = assistantStoneClient.getAssistantStoneInfo(tenantId, 1L);
//            }
//
//            ProductDetailDto productInfo = productClient.getProductInfo(tenantId, stockDto.getProductId(), storeInfo.getGrade());
//
//            ProductSnapshot product = stock.getProduct();
//            product.updateProduct(
//                    productInfo.getProductName(),
//                    productInfo.getLaborCost(),
//                    materialName,
//                    classificationName,
//                    colorName,
//                    setTypeName,
//                    stockDto.isAssistantStone(),
//                    assistantStoneInfo.getAssistantStoneId(),
//                    assistantStoneInfo.getAssistantStoneName(),
//                    stockDto.getAssistantStoneCreateAt()
//            );
//
////            List<Long> stoneIds = stockDto.getStoneIds();
////            for (Long stoneId : stoneIds) {
////                Boolean existStoneId = stoneClient.getExistStoneId(tenantId, stoneId);
////                if (!existStoneId) {
////                    throw new IllegalStateException(NOT_FOUND_STONE);
////                }
////            }
//
////            StoneUtil.updateStoneCostAndPurchase(stock);
////
////            int totalStonePurchaseCost = 0;
////            int mainStoneCost = 0;
////            int assistanceStoneCost = 0;
////            for (StoneDto.StoneInfo stoneInfo : stockDto.getStoneInfos()) {
////                Integer laborCost = stoneInfo.getLaborCost();
////                Integer quantity = stoneInfo.getQuantity();
////                Integer purchaseCost = stoneInfo.getPurchaseCost();
////                if (Boolean.TRUE.equals(stoneInfo.isIncludeStone())) {
////                    if (Boolean.TRUE.equals(stoneInfo.isMainStone())) {
////                        mainStoneCost += laborCost * quantity;
////                    } else {
////                        assistanceStoneCost += laborCost * quantity;
////                    }
////                    totalStonePurchaseCost += purchaseCost * quantity;
////                }
////            }
////
////            stock.updateStoneCost(totalStonePurchaseCost, mainStoneCost, assistanceStoneCost);
////            stock.updateAddStoneLaborCost(stockDto.getAddStoneLaborCost());
//            stock.updateStore(StoreDto.Response.builder().storeId(storeInfo.getStoreId()).storeName(storeInfo.getStoreName()).build());
//            stock.updateFactory(new FactoryDto.Response(factoryInfo.getFactoryId(), factoryInfo.getFactoryName(), factoryInfo.getFactoryHarry()));
//            stock.updateOrderStatus(OrderStatus.STOCK);
//
//            statusHistory = StatusHistory.phaseChange(
//                    stock.getFlowCode(),
//                    lastHistory.getSourceType(),
//                    lastHistory.getPhase(),
//                    BusinessPhase.STOCK,
//                    stockDto.getNickname()
//            );
//
//            statusHistoryRepository.save(statusHistory);
//
//        } catch (Exception e) {
//            log.error("Async failed. orderId={}, err={}", stockDto.getFlowCode(), e.getMessage(), e);
//
//            statusHistory = StatusHistory.phaseChange(
//                    stock.getFlowCode(),
//                    lastHistory.getSourceType(),
//                    lastHistory.getPhase(),
//                    BusinessPhase.STOCK_FAIL,
//                    stockDto.getNickname()
//            );
//
//            statusHistoryRepository.save(statusHistory);
//        }
//    }
//}
