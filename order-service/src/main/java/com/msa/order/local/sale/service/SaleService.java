package com.msa.order.local.sale.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.order.local.order.entity.StatusHistory;
import com.msa.order.local.order.entity.order_enum.BusinessPhase;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.order.repository.StatusHistoryRepository;
import com.msa.order.local.sale.entity.Sale;
import com.msa.order.local.sale.entity.SaleItem;
import com.msa.order.local.sale.repository.SaleItemRepository;
import com.msa.order.local.sale.repository.SaleRepository;
import com.msa.order.local.sale.sale_enum.SaleStatus;
import com.msa.order.local.stock.dto.StockDto;
import com.msa.order.local.stock.entity.Stock;
import com.msa.order.local.stock.repository.StockRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;
import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND_STOCK;
import static com.msa.order.local.order.util.StoneUtil.countStoneCost;
import static com.msa.order.local.order.util.StoneUtil.updateStoneInfo;

@Service
@Transactional
public class SaleService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final JwtUtil jwtUtil;
    private final StockRepository stockRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    public SaleService(JwtUtil jwtUtil, StockRepository stockRepository, SaleRepository saleRepository, SaleItemRepository saleItemRepository, StatusHistoryRepository statusHistoryRepository) {
        this.jwtUtil = jwtUtil;
        this.stockRepository = stockRepository;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    //주문 판매의 경우 sale_status 공유가 불가능한 stock에서는 사용 불가능 별도로 구현 필요
    // 주문에서 재고 등록을 한 후 바로 출고까지 넘기기
    public void createSaleFromOrder(String accessToken, Long flowCode) {
        String nickname = jwtUtil.getNickname(accessToken);

        Stock stock = stockRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_STOCK));

        if (saleItemRepository.existsByStock(stock)) {
            return;
        }

        if (!(stock.getOrderStatus() == OrderStatus.STOCK || stock.getOrderStatus() == OrderStatus.NORMAL)) {
            throw new IllegalStateException("판매로 전환 불가 상태: " + stock.getOrderStatus());
        }

        LocalDate saleDate = OffsetDateTime.now(KST).toLocalDate();
        Long storeId = stock.getStoreId();
        String storeName = stock.getStoreName();

        createNewSale(stock, saleDate, storeId, storeName);
        stock.updateOrderStatus(OrderStatus.SALE);

        updateNewHistory(flowCode, nickname);
    }

    public void createSaleFromStock(String accessToken, Long flowCode,  StockDto.stockRequest stockDto) {
        String nickname = jwtUtil.getNickname(accessToken);

        Stock stock = stockRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_STOCK));

        if (saleItemRepository.existsByStock(stock)) {
            return;
        }

        if (!(stock.getOrderStatus() == OrderStatus.STOCK || stock.getOrderStatus() == OrderStatus.NORMAL)) {
            throw new IllegalStateException("판매로 전환 불가 상태: " + stock.getOrderStatus());
        }

        List<StockDto.StoneInfo> stoneInfos = stockDto.getStoneInfos();
        updateStoneInfo(stoneInfos, stock);

        int totalStonePurchaseCost = 0;
        int mainStoneCost = 0;
        int assistanceStoneCost = 0;
        countStoneCost(stock.getOrderStones(), mainStoneCost, assistanceStoneCost, totalStonePurchaseCost);

        LocalDate saleDate = OffsetDateTime.now(KST).toLocalDate();
        Long storeId = stock.getStoreId();
        String storeName = stock.getStoreName();

        createNewSale(stock, saleDate, storeId, storeName);

        stock.updateOrderStatus(OrderStatus.SALE);

        updateNewHistory(flowCode, nickname);
    }

    //판매 제품 수정 로직 (중량, 단가 수정)

    //반품 로직

    //결제 관련 로직 생성

    //라벨 출력 기능

    private void createNewSale(Stock stock, LocalDate saleDate, Long storeId, String storeName) {
        Sale sale = saleRepository.findByStoreIdAndSaleDate(storeId, saleDate)
                .orElseGet(() -> {
                    try {
                        Sale newSale = Sale.builder()
                                .saleStatus(SaleStatus.SALE)
                                .storeId(storeId)
                                .storeName(storeName)
                                .items(new ArrayList<>())
                                .build();
                        return saleRepository.saveAndFlush(newSale);
                    } catch (DataIntegrityViolationException dup) {
                        // 동시 생성 충돌(UK_SALE_STORE_DATE) 시 재조회
                        return saleRepository.findByStoreIdAndSaleDate(storeId, saleDate)
                                .orElseThrow(() -> dup);
                    }
                });

        SaleItem saleItem = SaleItem.builder()
                .sale(sale)
                .stock(stock)
                .build();

        sale.getItems().add(saleItem);
    }

    private void updateNewHistory(Long flowCode, String nickname) {
        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        StatusHistory statusHistory = StatusHistory.phaseChange(
                flowCode,
                lastHistory.getSourceType(),
                BusinessPhase.valueOf(lastHistory.getToValue()),
                BusinessPhase.SALE,
                nickname
        );

        statusHistoryRepository.save(statusHistory);
    }

}
