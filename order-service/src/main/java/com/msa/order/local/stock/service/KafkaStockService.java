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
        final String token = stockDto.getToken();

        Stock stock = stockRepository.findByFlowCode(stockDto.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        if (stock.getOrderStatus() != OrderStatus.WAIT) {
            return;
        }

        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(stock.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        StatusHistory statusHistory;

        try {
            StoreDto.Response storeInfo;
            if (stockDto.getStoreId() != null && !stockDto.getStoreId().equals(stock.getStoreId())) {
                storeInfo = storeClient.getStoreInfo(token, stockDto.getStoreId());
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
                FactoryDto.Response latestFactoryInfo = factoryClient.getFactoryInfo(token, stockDto.getFactoryId());
                if (!latestFactoryInfo.getFactoryName().equals(stock.getFactoryName())) {
                    stock.updateFactory(new FactoryDto.Response(latestFactoryInfo.getFactoryId(), latestFactoryInfo.getFactoryName(), latestFactoryInfo.getFactoryHarry()));
                }
            }

            ProductSnapshot product = stock.getProduct();

            String latestMaterialName = product.getMaterialName();
            if (stockDto.getMaterialId() != null && !stockDto.getMaterialId().equals(product.getMaterialId())) {
                latestMaterialName = materialClient.getMaterialInfo(token, stockDto.getMaterialId());
            }

            String latestClassificationName = product.getClassificationName();
            if (stockDto.getClassificationId() != null && !stockDto.getClassificationId().equals(product.getClassificationId())) {
                latestClassificationName = classificationClient.getClassificationInfo(token, stockDto.getClassificationId());
            }

            String latestColorName = product.getColorName();
            if (stockDto.getColorId() != null && !stockDto.getColorId().equals(product.getColorId())) {
                latestColorName = colorClient.getColorInfo(token, stockDto.getColorId());
            }

            String latestSetTypeName = product.getSetTypeName();
            if (stockDto.getSetTypeId() != null && !stockDto.getSetTypeId().equals(product.getSetTypeId())) {
                latestSetTypeName = setTypeClient.getSetTypeName(token, stockDto.getSetTypeId());
            }

            AssistantStoneDto.Response latestAssistantStoneInfo;
            if (stockDto.getAssistantStoneId() != null && !stockDto.getAssistantStoneId().equals(product.getAssistantStoneId())) {
                latestAssistantStoneInfo = assistantStoneClient.getAssistantStoneInfo(token, stockDto.getAssistantStoneId());
            } else {
                latestAssistantStoneInfo = new AssistantStoneDto.Response(product.getAssistantStoneId(), product.getAssistantStoneName(), "");
            }

            ProductDetailDto latestProductInfo = productClient.getProductInfo(token, stockDto.getProductId(), storeInfo.getGrade());

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
                        latestProductInfo.getPurchaseCost(),
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
            stockRepository.save(stock);

            statusHistory = StatusHistory.phaseChange(
                    stock.getFlowCode(),
                    lastHistory.getSourceType(),
                    BusinessPhase.valueOf(lastHistory.getToValue()),
                    BusinessPhase.STOCK,
                    stockDto.getNickname()
            );

            statusHistoryRepository.save(statusHistory);

        } catch (Exception e) {
            statusHistory = StatusHistory.phaseChange(
                    stock.getFlowCode(),
                    lastHistory.getSourceType(),
                    BusinessPhase.valueOf(lastHistory.getToValue()),
                    BusinessPhase.FAIL,
                    stockDto.getNickname()
            );

            statusHistoryRepository.save(statusHistory);
        }
    }
}