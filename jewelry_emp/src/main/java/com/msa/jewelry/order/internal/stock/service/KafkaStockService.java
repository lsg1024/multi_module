package com.msa.jewelry.order.internal.stock.service;

import com.msa.jewelry.account.api.FactoryFinder;
import com.msa.jewelry.account.api.FactoryView;
import com.msa.jewelry.account.api.StoreFinder;
import com.msa.jewelry.account.api.StoreView;
import com.msa.jewelry.order.internal.global.kafka_dto_legacy.KafkaStockRequest;
import com.msa.jewelry.order.internal.global.util.SafeParse;
import com.msa.jewelry.order.internal.order.entity.StatusHistory;
import com.msa.jewelry.order.internal.order.entity.order_enum.BusinessPhase;
import com.msa.jewelry.order.internal.order.entity.order_enum.OrderStatus;
import com.msa.jewelry.order.internal.order.repository.StatusHistoryRepository;
import com.msa.jewelry.order.internal.stock.entity.ProductSnapshot;
import com.msa.jewelry.order.internal.stock.entity.Stock;
import com.msa.jewelry.order.internal.stock.repository.StockRepository;
import com.msa.jewelry.product.api.AssistantStoneFinder;
import com.msa.jewelry.product.api.AssistantStoneView;
import com.msa.jewelry.product.api.ClassificationFinder;
import com.msa.jewelry.product.api.ColorFinder;
import com.msa.jewelry.product.api.MaterialFinder;
import com.msa.jewelry.product.api.ProductDetailView;
import com.msa.jewelry.product.api.ProductFinder;
import com.msa.jewelry.product.api.SetTypeFinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static com.msa.jewelry.order.internal.global.exception.ExceptionMessage.NOT_FOUND;

@Slf4j
@Service
public class KafkaStockService {

    private final StoreFinder storeFinder;
    private final FactoryFinder factoryFinder;
    private final ProductFinder productFinder;
    private final MaterialFinder materialFinder;
    private final ColorFinder colorFinder;
    private final SetTypeFinder setTypeFinder;
    private final ClassificationFinder classificationFinder;
    private final AssistantStoneFinder assistantStoneFinder;
    private final StockRepository stockRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    public KafkaStockService(StoreFinder storeFinder, FactoryFinder factoryFinder, ProductFinder productFinder, MaterialFinder materialFinder, ColorFinder colorFinder, SetTypeFinder setTypeFinder, ClassificationFinder classificationFinder, AssistantStoneFinder assistantStoneFinder, StockRepository stockRepository, StatusHistoryRepository statusHistoryRepository) {
        this.storeFinder = storeFinder;
        this.factoryFinder = factoryFinder;
        this.productFinder = productFinder;
        this.materialFinder = materialFinder;
        this.colorFinder = colorFinder;
        this.setTypeFinder = setTypeFinder;
        this.classificationFinder = classificationFinder;
        this.assistantStoneFinder = assistantStoneFinder;
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
            // 거래처 정보 — 변경된 경우 최신 정보 조회, 아니면 기존 스냅샷 사용.
            // 2026-05 P4: Stock 의 storeName/factoryName 컬럼 제거. id 변경 시점에만 스냅샷 갱신.
            String resolvedStoreGrade;
            if (stockDto.getStoreId() != null && !stockDto.getStoreId().equals(stock.getStoreId())) {
                StoreView storeInfo = storeFinder.getStoreInfo(stockDto.getStoreId());
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
                FactoryView latestFactoryInfo = factoryFinder.getFactoryInfo(stockDto.getFactoryId());
                stock.updateFactory(
                        latestFactoryInfo.factoryId(),
                        SafeParse.toBigDecimalOrNull(latestFactoryInfo.goldHarryLoss())
                );
            }

            ProductSnapshot product = stock.getProduct();

            String latestMaterialName = product.getMaterialName();
            if (stockDto.getMaterialId() != null && !stockDto.getMaterialId().equals(product.getMaterialId())) {
                latestMaterialName = materialFinder.getMaterialName(stockDto.getMaterialId());
            }

            String latestClassificationName = product.getClassificationName();
            if (stockDto.getClassificationId() != null && !stockDto.getClassificationId().equals(product.getClassificationId())) {
                latestClassificationName = classificationFinder.getClassificationName(stockDto.getClassificationId());
            }

            String latestColorName = product.getColorName();
            if (stockDto.getColorId() != null && !stockDto.getColorId().equals(product.getColorId())) {
                latestColorName = colorFinder.getColorName(stockDto.getColorId());
            }

            String latestSetTypeName = product.getSetTypeName();
            if (stockDto.getSetTypeId() != null && !stockDto.getSetTypeId().equals(product.getSetTypeId())) {
                latestSetTypeName = setTypeFinder.getSetTypeName(stockDto.getSetTypeId());
            }

            // 보조석 정보 — 변경 시 최신 조회, 아니면 기존 스냅샷에서 view 구성
            AssistantStoneView latestAssistantStoneInfo;
            if (stockDto.getAssistantStoneId() != null && !stockDto.getAssistantStoneId().equals(product.getAssistantStoneId())) {
                latestAssistantStoneInfo = assistantStoneFinder.getAssistantStone(stockDto.getAssistantStoneId());
            } else {
                latestAssistantStoneInfo = new AssistantStoneView(
                        product.getAssistantStoneId(),
                        product.getAssistantStoneName(),
                        ""
                );
            }

            ProductDetailView latestProductInfo = productFinder.getProductDetail(stockDto.getProductId(), resolvedStoreGrade);

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
