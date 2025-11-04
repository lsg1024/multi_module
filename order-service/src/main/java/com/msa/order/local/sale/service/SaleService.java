package com.msa.order.local.sale.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.util.CustomPage;
import com.msa.order.global.dto.StoneDto;
import com.msa.order.global.kafka.KafkaProducer;
import com.msa.order.global.kafka.dto.AccountDto;
import com.msa.order.global.util.GoldUtils;
import com.msa.order.local.order.entity.StatusHistory;
import com.msa.order.local.order.entity.order_enum.BusinessPhase;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.order.repository.CustomOrderStoneRepository;
import com.msa.order.local.order.repository.StatusHistoryRepository;
import com.msa.order.local.sale.entity.Sale;
import com.msa.order.local.sale.entity.SaleItem;
import com.msa.order.local.sale.entity.SalePayment;
import com.msa.order.local.sale.entity.dto.SaleDto;
import com.msa.order.local.sale.entity.dto.SaleRow;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.msa.order.global.exception.ExceptionMessage.NOT_ACCESS;
import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;
import static com.msa.order.local.order.util.StoneUtil.countStoneCost;
import static com.msa.order.local.order.util.StoneUtil.updateStockStoneInfo;

@Service
@Transactional
public class SaleService {
    private final JwtUtil jwtUtil;
    private final KafkaProducer kafkaProducer;
    private final StockRepository stockRepository;
    private final SaleRepository saleRepository;
    private final CustomOrderStoneRepository customOrderStoneRepository;
    private final SaleItemRepository saleItemRepository;
    private final SalePaymentRepository salePaymentRepository;
    private final CustomSaleRepository customSaleRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    public SaleService(JwtUtil jwtUtil, KafkaProducer kafkaProducer, StockRepository stockRepository, SaleRepository saleRepository, CustomOrderStoneRepository customOrderStoneRepository, SaleItemRepository saleItemRepository, SalePaymentRepository salePaymentRepository, CustomSaleRepository customSaleRepository, StatusHistoryRepository statusHistoryRepository) {
        this.jwtUtil = jwtUtil;
        this.kafkaProducer = kafkaProducer;
        this.stockRepository = stockRepository;
        this.saleRepository = saleRepository;
        this.customOrderStoneRepository = customOrderStoneRepository;
        this.saleItemRepository = saleItemRepository;
        this.salePaymentRepository = salePaymentRepository;
        this.customSaleRepository = customSaleRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    public CustomPage<SaleRow> getSale(String input, String startAt, String endAt, String material, Pageable pageable) {
        SaleDto.Condition condition = new SaleDto.Condition(input, startAt, endAt, material);
        return customSaleRepository.findSales(condition, pageable);
    }

    // 주문 판매의 경우 sale_status 공유가 불가능한 stock에서는 사용 불가능 별도로 구현 필요
    // 주문에서 재고 등록을 한 후 바로 출고까지 넘기기 -> 만약 개별 명세서를 만들고 싶은 경우(동일 매입처인 경우)
    public void orderToSale(String accessToken, Long flowCode, StockDto.StockRegisterRequest stockDto) {
        String nickname = jwtUtil.getNickname(accessToken);
        String tenantId = jwtUtil.getTenantId(accessToken);

        Stock stock = stockRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
        if (saleItemRepository.existsByStock(stock)) {
            return;
        }

        if (!(stock.getOrderStatus() == OrderStatus.STOCK || stock.getOrderStatus() == OrderStatus.NORMAL)) {
            throw new IllegalStateException("판매로 전환 불가 상태: " + stock.getOrderStatus());
        }

        LocalDateTime saleDate = LocalDateTime.now();
        Long storeId = stock.getStoreId();
        String storeName = stock.getStoreName();

        createNewSale(stock, saleDate, storeId, storeName);
        stock.updateOrderStatus(OrderStatus.SALE);


        updateNewHistory(stock.getFlowCode(), nickname);

        BigDecimal pureGoldWeight = GoldUtils.calculatePureGoldWeight(stockDto.getGoldWeight(), stockDto.getMaterialName().toUpperCase());

        AccountDto.updateCurrentBalance dto = AccountDto.updateCurrentBalance.builder()
                .eventId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .saleType("SALE")
                .type("STORE")
                .id(storeId)
                .name(storeName)
                .goldBalance(pureGoldWeight)
                .moneyBalance(stock.getTotalStoneLaborCost())
                .build();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaProducer.currentBalanceUpdate(dto);
            }
        });
    }

    // 재고 -> 판매
    public void stockToSale(String accessToken, Long flowCode, StockDto.stockRequest stockDto) {
        String nickname = jwtUtil.getNickname(accessToken);

        Stock stock = stockRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
        if (saleItemRepository.existsByStock(stock)) {
            return;
        }

        if (!(stock.getOrderStatus() == OrderStatus.STOCK || stock.getOrderStatus() == OrderStatus.NORMAL)) {
            throw new IllegalStateException("판매로 전환 불가 상태: " + stock.getOrderStatus());
        }

        List<StoneDto.StoneInfo> stoneInfos = stockDto.getStoneInfos();
        updateStockStoneInfo(stoneInfos, stock);

        int[] countStoneCost = countStoneCost(stock.getOrderStones());
        stock.updateStoneCost(countStoneCost[0], countStoneCost[1], countStoneCost[2], countStoneCost[3], stockDto.getStoneAddLaborCost());

        LocalDateTime saleDate = LocalDateTime.now();
        Long storeId = stock.getStoreId();
        String storeName = stock.getStoreName();

        createNewSale(stock, saleDate, storeId, storeName);

        stock.updateOrderStatus(OrderStatus.SALE);
        stock.updateStockNote(stockDto.getStockNote(), stockDto.getMainStoneNote(), stockDto.getAssistanceStoneNote());
        stock.getProduct().updateProductWeightAndSize(stockDto.getProductSize(), new BigDecimal(stockDto.getGoldWeight()), new BigDecimal(stockDto.getStoneWeight()));

        updateNewHistory(stock.getFlowCode(), nickname);

        // 별도 정산 로직에서 Kafka를 통해 갱신 데이터를 dto에 포함에 전달 정합성도 고려해야함
    }

    //판매 제품 수정 로직 (중량, 단가 수정)
    public void createPayment(String accessToken, String idempKey, SaleDto.Request saleDto) {
        salePaymentRepository.findByIdempotencyKey(idempKey).ifPresent(p -> {
            throw new IllegalStateException("이미 등록된 요청입니다.");
        });

        String tenantId = jwtUtil.getTenantId(accessToken);

        LocalDateTime saleDate = LocalDateTime.now();
        Long storeId = saleDto.getId();
        String storeName = saleDto.getName();

        Sale sale = saleRepository.findByStoreIdAndCreateDate(storeId, saleDate)
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
                        return saleRepository.findByStoreIdAndCreateDate(storeId, saleDate)
                                .orElseThrow(() -> dup);
                    }
                });

        final Integer cashAmount = saleDto.getPayAmount();
        final String material = saleDto.getMaterial();
        final BigDecimal snapTotalWeight = saleDto.getGoldWeight();
        final String note = saleDto.getSaleNote();

        SaleStatus saleStatus;
        try {
            saleStatus = SaleStatus.valueOf(saleDto.getOrderStatus());
        } catch (IllegalArgumentException e) {
            saleStatus = SaleStatus.fromDisplayName(saleDto.getOrderStatus())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown payment type: " + saleDto.getOrderStatus()));
        }

        SalePayment payment = switch (saleStatus) {
            case PAYMENT -> SalePayment.payment(
                    material, idempKey, note,
                    cashAmount,
                    snapTotalWeight
            );
            case WG -> SalePayment.wg(
                    material, idempKey, note,
                    cashAmount,
                    snapTotalWeight
            );
            case PAYMENT_TO_BANK -> SalePayment.paymentBank(
                    material, idempKey, note,
                    cashAmount
            );
            case DISCOUNT -> SalePayment.discount(
                    material, idempKey, note,
                    cashAmount,
                    snapTotalWeight
            );
            default -> throw new IllegalArgumentException("Unsupported payment type: " + saleStatus);
        };

        sale.addPayment(payment);
        salePaymentRepository.save(payment);

        AccountDto.updateCurrentBalance dto = AccountDto.updateCurrentBalance.builder()
                .eventId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .saleType(saleDto.getOrderStatus())
                .type("STORE")
                .id(storeId)
                .name(storeName)
                .goldBalance(snapTotalWeight)
                .moneyBalance(cashAmount)
                .build();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaProducer.currentBalanceUpdate(dto);
            }
        });
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


    private void createNewSale(Stock stock, LocalDateTime saleDate, Long storeId, String storeName) {
        Sale sale = saleRepository.findByStoreIdAndCreateDate(storeId, saleDate)
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
                        return saleRepository.findByStoreIdAndCreateDate(storeId, saleDate)
                                .orElseThrow(() -> dup);
                    }
                });

        SaleItem saleItem = SaleItem.builder()
                .flowCode(stock.getFlowCode())
                .saleCode(sale.getSaleCode())
                .build();

        sale.addItem(saleItem);
        saleItem.setStock(stock);
        saleItemRepository.save(saleItem);
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
