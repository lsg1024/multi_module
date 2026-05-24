package com.msa.jewelry.local.stock.service;

import com.msa.jewelry.local.factory.service.FactoryService;
import com.msa.jewelry.local.factory.dto.FactoryView;
import com.msa.jewelry.local.store.service.StoreService;
import com.msa.jewelry.local.store.dto.StoreView;
import com.msa.jewelry.local.stock.dto.KafkaStockRequest;
import com.msa.jewelry.global.util.SafeParse;
import com.msa.jewelry.local.order.entity.StatusHistory;
import com.msa.jewelry.local.order.entity.order_enum.BusinessPhase;
import com.msa.jewelry.local.order.entity.order_enum.OrderStatus;
import com.msa.jewelry.local.order.repository.StatusHistoryRepository;
import com.msa.jewelry.local.stock.entity.ProductSnapshot;
import com.msa.jewelry.local.stock.entity.Stock;
import com.msa.jewelry.local.stock.repository.StockRepository;
import com.msa.jewelry.local.assistant_stone.service.AssistantStoneService;
import com.msa.jewelry.local.assistant_stone.dto.AssistantStoneView;
import com.msa.jewelry.local.classification.service.ClassificationService;
import com.msa.jewelry.local.color.service.ColorService;
import com.msa.jewelry.local.material.service.MaterialService;
import com.msa.jewelry.local.product.dto.ProductDetailView;
import com.msa.jewelry.local.product.service.ProductService;
import com.msa.jewelry.local.set.service.SetTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static com.msa.jewelry.global.exception.ExceptionMessage.NOT_FOUND;

@Slf4j
@Service
public class KafkaStockService {

    private final StoreService storeService;
    private final FactoryService factoryService;
    private final ProductService productService;
    private final MaterialService materialService;
    private final ColorService colorService;
    private final SetTypeService setTypeService;
    private final ClassificationService classificationService;
    private final AssistantStoneService assistantStoneService;
    private final StockRepository stockRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    public KafkaStockService(StoreService storeService, FactoryService factoryService, ProductService productService, MaterialService materialService, ColorService colorService, SetTypeService setTypeService, ClassificationService classificationService, AssistantStoneService assistantStoneService, StockRepository stockRepository, StatusHistoryRepository statusHistoryRepository) {
        this.storeService = storeService;
        this.factoryService = factoryService;
        this.productService = productService;
        this.materialService = materialService;
        this.colorService = colorService;
        this.setTypeService = setTypeService;
        this.classificationService = classificationService;
        this.assistantStoneService = assistantStoneService;
        this.stockRepository = stockRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    @Transactional
    public void saveStock(KafkaStockRequest stockDto) {
        Stock stock = stockRepository.findByFlowCode(stockDto.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        if (stock.getOrderStatus() != OrderStatus.WAIT) {
            return;
        }

        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(stock.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        StatusHistory statusHistory;

        try {
            String resolvedStoreGrade;
            if (stockDto.getStoreId() != null && !stockDto.getStoreId().equals(stock.getStoreId())) {
                StoreView storeInfo = storeService.getStoreInfoView(stockDto.getStoreId());
                stock.updateStore(
                        storeInfo.storeId(),
                        storeInfo.storeGrade(),
                        SafeParse.toBigDecimalOrNull(storeInfo.storeHarry())
                );
                resolvedStoreGrade = storeInfo.storeGrade();
            } else {
                resolvedStoreGrade = stock.getStoreGrade();
            }

            // 공장 정보
            if (stockDto.getFactoryId() != null && !stockDto.getFactoryId().equals(stock.getFactoryId())) {
                FactoryView latestFactoryInfo = factoryService.getFactoryInfo(stockDto.getFactoryId());
                stock.updateFactory(
                        latestFactoryInfo.factoryId(),
                        SafeParse.toBigDecimalOrNull(latestFactoryInfo.goldHarryLoss())
                );
            }

            ProductSnapshot product = stock.getProduct();

            String latestMaterialName = product.getMaterialName();
            if (stockDto.getMaterialId() != null && !stockDto.getMaterialId().equals(product.getMaterialId())) {
                latestMaterialName = materialService.getMaterialName(stockDto.getMaterialId());
            }

            String latestClassificationName = product.getClassificationName();
            if (stockDto.getClassificationId() != null && !stockDto.getClassificationId().equals(product.getClassificationId())) {
                latestClassificationName = classificationService.getClassificationName(stockDto.getClassificationId());
            }

            String latestColorName = product.getColorName();
            if (stockDto.getColorId() != null && !stockDto.getColorId().equals(product.getColorId())) {
                latestColorName = colorService.getColorName(stockDto.getColorId());
            }

            String latestSetTypeName = product.getSetTypeName();
            if (stockDto.getSetTypeId() != null && !stockDto.getSetTypeId().equals(product.getSetTypeId())) {
                latestSetTypeName = setTypeService.getSetTypeName(stockDto.getSetTypeId());
            }

            AssistantStoneView latestAssistantStoneInfo;
            if (stockDto.getAssistantStoneId() != null && !stockDto.getAssistantStoneId().equals(product.getAssistantStoneId())) {
                latestAssistantStoneInfo = assistantStoneService.getAssistantStoneView(stockDto.getAssistantStoneId());
            } else {
                latestAssistantStoneInfo = new AssistantStoneView(
                        product.getAssistantStoneId(),
                        product.getAssistantStoneName(),
                        ""
                );
            }

            ProductDetailView latestProductInfo = productService.getProductDetail(stockDto.getProductId(), resolvedStoreGrade);

            if (!Objects.equals(latestProductInfo.productName(), product.getProductName()) ||
                    !Objects.equals(latestMaterialName, product.getMaterialName()) ||
                    !Objects.equals(latestClassificationName, product.getClassificationName()) ||
                    !Objects.equals(latestColorName, product.getColorName()) ||
                    !Objects.equals(latestSetTypeName, product.getSetTypeName()) ||
                    !Objects.equals(latestAssistantStoneInfo.assistantStoneName(), product.getAssistantStoneName())) {

                product.updateProduct(
                        latestProductInfo.productName(),
                        latestMaterialName,
                        latestClassificationName,
                        latestColorName,
                        latestSetTypeName,
                        stockDto.isAssistantStone(),
                        latestAssistantStoneInfo.assistantStoneId(),
                        latestAssistantStoneInfo.assistantStoneName(),
                        stockDto.getAssistantStoneCreateAt()
                );
            }

            stock.updateOrderStatus(OrderStatus.STOCK);
            stockRepository.save(stock);

        } catch (Exception e) {
            statusHistory = StatusHistory.phaseChange(
                    stock.getFlowCode(),
                    lastHistory.getSourceType(),
                    BusinessPhase.valueOf(lastHistory.getToValue()),
                    BusinessPhase.FAIL,
                    "재고 등록 실패",
                    stockDto.getNickname()
            );

            statusHistoryRepository.save(statusHistory);
        }
    }
}
