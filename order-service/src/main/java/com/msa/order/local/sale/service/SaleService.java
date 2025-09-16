package com.msa.order.local.sale.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.util.CustomPage;
import com.msa.order.global.dto.StoneDto;
import com.msa.order.local.order.entity.OrderStone;
import com.msa.order.local.order.entity.StatusHistory;
import com.msa.order.local.order.entity.order_enum.BusinessPhase;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.order.repository.CustomOrderStoneRepository;
import com.msa.order.local.order.repository.StatusHistoryRepository;
import com.msa.order.local.sale.entity.Sale;
import com.msa.order.local.sale.entity.SaleItem;
import com.msa.order.local.sale.entity.SalePayment;
import com.msa.order.local.sale.entity.dto.SaleDto;
import com.msa.order.local.sale.repository.CustomSaleRepository;
import com.msa.order.local.sale.repository.SaleItemRepository;
import com.msa.order.local.sale.repository.SalePaymentRepository;
import com.msa.order.local.sale.repository.SaleRepository;
import com.msa.order.local.sale.sale_enum.SaleStatus;
import com.msa.order.local.stock.dto.StockDto;
import com.msa.order.local.stock.entity.Stock;
import com.msa.order.local.stock.repository.StockRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.msa.order.global.exception.ExceptionMessage.*;
import static com.msa.order.local.order.util.StoneUtil.countStoneCost;
import static com.msa.order.local.order.util.StoneUtil.updateStoneInfo;

@Service
@Transactional
public class SaleService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final JwtUtil jwtUtil;
    private final StockRepository stockRepository;
    private final SaleRepository saleRepository;
    private final CustomOrderStoneRepository customOrderStoneRepository;
    private final SaleItemRepository saleItemRepository;
    private final SalePaymentRepository salePaymentRepository;
    private final CustomSaleRepository customSaleRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    public SaleService(JwtUtil jwtUtil, StockRepository stockRepository, SaleRepository saleRepository, CustomOrderStoneRepository customOrderStoneRepository, SaleItemRepository saleItemRepository, SalePaymentRepository salePaymentRepository, CustomSaleRepository customSaleRepository, StatusHistoryRepository statusHistoryRepository) {
        this.jwtUtil = jwtUtil;
        this.stockRepository = stockRepository;
        this.saleRepository = saleRepository;
        this.customOrderStoneRepository = customOrderStoneRepository;
        this.saleItemRepository = saleItemRepository;
        this.salePaymentRepository = salePaymentRepository;
        this.customSaleRepository = customSaleRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    public CustomPage<SaleDto.Response> getSale(SaleDto.Condition condition, Pageable pageable) {
        return customSaleRepository.findSales(condition, pageable);
    }

    // 주문 판매의 경우 sale_status 공유가 불가능한 stock에서는 사용 불가능 별도로 구현 필요
    // 주문에서 재고 등록을 한 후 바로 출고까지 넘기기 -> 만약 개별 명세서를 만들고 싶은 경우(동일 매입처인 경우)
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

        createNewSale(nickname, stock, saleDate, storeId, storeName);
        stock.updateOrderStatus(OrderStatus.SALE);

        updateNewHistory(flowCode, nickname, OrderStatus.SALE.name());

        //별도 정산 로직에서 Kafka를 통해 갱신 데이터를 dto에 포함에 전달 정합성도 고려해야함
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

        List<StoneDto.StoneInfo> stoneInfos = stockDto.getStoneInfos();
        List<OrderStone> orderStones = stock.getOrderStones();
        updateStoneInfo(stoneInfos, stock, orderStones);

        int totalStonePurchaseCost = 0;
        int mainStoneCost = 0;
        int assistanceStoneCost = 0;
        countStoneCost(stock.getOrderStones(), mainStoneCost, assistanceStoneCost, totalStonePurchaseCost);

        LocalDate saleDate = OffsetDateTime.now(KST).toLocalDate();
        Long storeId = stock.getStoreId();
        String storeName = stock.getStoreName();

        createNewSale(nickname, stock, saleDate, storeId, storeName);

        stock.updateOrderStatus(OrderStatus.SALE);
        stock.updateStockInfo(stockDto);

        updateNewHistory(flowCode, nickname, OrderStatus.SALE.name());

        //별도 정산 로직에서 Kafka를 통해 갱신 데이터를 dto에 포함에 전달 정합성도 고려해야함
    }

    //판매 제품 수정 로직 (중량, 단가 수정)
    public void createPayment(String accessToken, String idempKey, SaleDto.Request saleDto) {
        salePaymentRepository.findByIdempotencyKey(idempKey).ifPresent(p -> {
            throw new IllegalStateException("이미 등록된 요청입니다.");
        });

        String nickname = jwtUtil.getNickname(accessToken);

        LocalDate saleDate = OffsetDateTime.now(KST).toLocalDate();
        Long storeId = saleDto.getStoreId();
        String storeName = saleDto.getStoreName();

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

        final Long snapTotalPay = saleDto.getTotalPay();
        final String material = saleDto.getMaterial();
        final BigDecimal snapTotalWeight = saleDto.getTotalWeight();
        final String note = saleDto.getSaleNote();

        SaleStatus saleStatus;
        try {
            saleStatus = SaleStatus.valueOf(saleDto.getType());
        } catch (IllegalArgumentException e) {
            saleStatus = SaleStatus.fromDisplayName(saleDto.getType())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown payment type: " + saleDto.getType()));
        }

        SalePayment payment = switch (saleStatus) {
            case PAYMENT -> SalePayment.payment(
                    material, nickname, idempKey, note,
                    snapTotalPay,
                    snapTotalWeight
            );
            case WG -> SalePayment.wg(
                    material, nickname, idempKey, note,
                    snapTotalPay,
                    snapTotalWeight
            );
            case PAYMENT_TO_BANK -> SalePayment.paymentBank(
                    material, nickname, idempKey, note,
                    snapTotalPay
            );
            case DISCOUNT -> SalePayment.discount(
                    material, nickname, idempKey, note,
                    snapTotalPay,
                    snapTotalWeight
            );
            default -> throw new IllegalArgumentException("Unsupported payment type: " + saleStatus);
        };

        sale.addPayment(payment);
        salePaymentRepository.save(payment);

        // 카프카를 통해 재무 서버에 메시지
    }

    //반품 로직 -> 제품은 다시 재고로, 결제는 다시 원복 -> 마지막 결제일의 경우?
    public void cancelSale(String accessToken, Long flowCode) {
        String role = jwtUtil.getRole(accessToken);
        if (role.equals("WAIT")) {
            throw new IllegalStateException(NOT_ACCESS);
        }

        SalePayment salePayment = salePaymentRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        Sale sale;
        switch (salePayment.getSaleStatus()) {
            case PAYMENT, PAYMENT_TO_BANK, DISCOUNT, WG -> {

                salePaymentRepository.delete(salePayment);

                sale = salePayment.getSale();

                //별도 정산 로직에서 Kafka를 통해 갱신 데이터를 dto에 포함에 전달 정합성도 고려해야함

                cleanupEmptySaleIfNeeded(sale);
            }
            case SALE -> {
                SaleItem saleItem = saleItemRepository.findBySaleCode(salePayment.getSaleCode())
                        .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

                sale = saleItem.getSale();
                Stock stock = saleItem.getStock();

                saleItemRepository.delete(saleItem);

                stock.updateOrderStatus(OrderStatus.STOCK);
                stockRepository.save(stock);

                //별도 정산 로직에서 Kafka를 통해 갱신 데이터를 dto에 포함에 전달 정합성도 고려해야함

                cleanupEmptySaleIfNeeded(sale);
            }
        }

    }

    //라벨 출력 기능


    private void createNewSale(String nickname, Stock stock, LocalDate saleDate, Long storeId, String storeName) {
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
                .createdBy(nickname)
                .flowCode(stock.getFlowCode())
                .saleCode(sale.getSaleCode())
                .build();

        sale.addItem(saleItem);
        saleItem.setStock(stock);
        saleItemRepository.save(saleItem);
    }

    private void updateNewHistory(Long flowCode, String nickname, String status) {
        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        StatusHistory statusHistory = StatusHistory.phaseChange(
                flowCode,
                lastHistory.getSourceType(),
                lastHistory.getPhase(),
                BusinessPhase.valueOf(status),
                nickname
        );

        statusHistoryRepository.save(statusHistory);
    }

    private void cleanupEmptySaleIfNeeded(Sale sale) {
        boolean hasItems    = saleItemRepository.existsBySale(sale);
        boolean hasPayments = salePaymentRepository.existsBySale(sale);
        if (!hasItems && !hasPayments) {
            saleRepository.delete(sale);
        }
    }

    public List<SaleDto.SaleDetailDto> findSaleProductNameAndMaterial(Long storeId, Long productId, String materialName) {
        List<SaleDto.SaleDetailDto> saleDetailDtos = customSaleRepository.findSalePast(storeId, productId, materialName);

        if (saleDetailDtos.isEmpty()) {
            return saleDetailDtos;
        }

        List<Long> flowCodes = saleDetailDtos.stream()
                .map(SaleDto.SaleDetailDto::getFlowCode)
                .distinct()
                .toList();

        List<SaleDto.StoneCountDto> stoneCounts = customOrderStoneRepository.findStoneCountsByStockIds(flowCodes);

        Map<Long, Map<Boolean, Integer>> stoneCountMaps = stoneCounts.stream()
                .collect(Collectors.groupingBy(
                        SaleDto.StoneCountDto::getFlowCode,
                        Collectors.toMap(SaleDto.StoneCountDto::getMainStone, SaleDto.StoneCountDto::getTotalQuantity)
                ));

        saleDetailDtos.forEach(dto -> {
            Map<Boolean, Integer> counts = stoneCountMaps.get(dto.getFlowCode());
            if (counts != null) {
                dto.setMainStoneQuantity(counts.getOrDefault(true, 0));
                dto.setAssistanceStoneQuantity(counts.getOrDefault(false, 0));
            } else {
                dto.setMainStoneQuantity(0);
                dto.setAssistanceStoneQuantity(0);
            }
        });

        return saleDetailDtos;
    }
}
