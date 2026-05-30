package com.msa.jewelry.local.sale.service;

import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.global.exception.ExceptionMessage;
import com.msa.jewelry.local.factory.dto.FactoryView;
import com.msa.jewelry.local.factory.service.FactoryService;
import com.msa.jewelry.local.order.entity.StatusHistory;
import com.msa.jewelry.local.order.entity.order_enum.OrderStatus;
import com.msa.jewelry.local.order.entity.order_enum.SourceType;
import com.msa.jewelry.local.order.entity.order_enum.BusinessPhase;
import com.msa.jewelry.local.order.repository.CustomOrderStoneRepository;
import com.msa.jewelry.local.order.repository.StatusHistoryRepository;
import com.msa.jewelry.local.product.dto.ProductImageView;
import com.msa.jewelry.local.product.service.ProductService;
import com.msa.jewelry.local.sale.dto.SaleDto;
import com.msa.jewelry.local.sale.dto.SaleItemResponse;
import com.msa.jewelry.local.sale.dto.SalePrintResponse;
import com.msa.jewelry.local.sale.entity.Sale;
import com.msa.jewelry.local.sale.entity.SaleItem;
import com.msa.jewelry.local.sale.entity.SalePayment;
import com.msa.jewelry.local.sale.repository.CustomSaleRepository;
import com.msa.jewelry.local.sale.repository.SaleItemRepository;
import com.msa.jewelry.local.sale.repository.SalePaymentRepository;
import com.msa.jewelry.local.sale.repository.SaleRepository;
import com.msa.jewelry.local.stock.dto.StockDto;
import com.msa.jewelry.local.stock.entity.ProductSnapshot;
import com.msa.jewelry.local.stock.entity.Stock;
import com.msa.jewelry.local.stock.repository.StockRepository;
import com.msa.jewelry.local.store.dto.StoreReceivableLogView;
import com.msa.jewelry.local.store.dto.StoreView;
import com.msa.jewelry.local.store.service.StoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SaleService 단위 테스트")
class SaleServiceTest {

    private static final String TOKEN     = "Bearer test-token";
    private static final String TENANT_ID = "tenant-001";
    private static final String NICKNAME  = "tester";
    private static final String EVENT_ID  = "evt_unit_test_001";
    private static final Long   STORE_ID  = 100L;
    private static final Long   FACTORY_ID = 200L;
    private static final Long   FLOW_CODE = 9_990L;
    private static final Long   SALE_CODE = 5_550L;

    @Mock JwtUtil jwtUtil;
    @Mock StoreService storeService;
    @Mock FactoryService factoryService;
    @Mock StockRepository stockRepository;
    @Mock SaleRepository saleRepository;
    @Mock CustomOrderStoneRepository customOrderStoneRepository;
    @Mock SaleItemRepository saleItemRepository;
    @Mock SalePaymentRepository salePaymentRepository;
    @Mock CustomSaleRepository customSaleRepository;
    @Mock StatusHistoryRepository statusHistoryRepository;
    @Mock ProductService productService;

    @InjectMocks
    SaleService saleService;

    @BeforeEach
    void commonStubs() {
        given(jwtUtil.getTenantId(anyString())).willReturn(TENANT_ID);
        given(jwtUtil.getNickname(anyString())).willReturn(NICKNAME);
    }

    // -----------------------------------------------------------------------
    // getDetailSale
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getDetailSale")
    class GetDetailSale {

        @Test
        @DisplayName("SALE 상태 — saleItem 조회 후 Response 정상 반환")
        void sale_정상조회() {
            Stock stock = stubStock();
            SaleItem saleItem = mock(SaleItem.class);
            given(saleItem.getFlowCode()).willReturn(FLOW_CODE);
            given(saleItem.getStock()).willReturn(stock);
            given(saleItem.getCreateDate()).willReturn(LocalDateTime.now());
            given(saleItem.getItemStatus()).willReturn(SaleStatus.SALE);

            Sale parent = mock(Sale.class);
            given(parent.getAccountGoldPrice()).willReturn(85_000);
            given(saleItem.getSale()).willReturn(parent);

            given(saleItemRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(saleItem));
            given(storeService.getStoreInfoView(STORE_ID))
                    .willReturn(new StoreView(STORE_ID, "강남금은방", "A", "1.5", "SELL", false));

            SaleDto.Response resp = saleService.getDetailSale(FLOW_CODE, SaleStatus.SALE.name());

            assertThat(resp).isNotNull();
            assertThat(resp.getFlowCode()).isEqualTo(FLOW_CODE);
            assertThat(resp.getSaleType()).isEqualTo(SaleStatus.SALE.name());
            assertThat(resp.getName()).isEqualTo("강남금은방");
        }

        @Test
        @DisplayName("SALE 상태 — saleItem 없음 → IllegalArgumentException(NOT_FOUND)")
        void sale_없음_예외() {
            given(saleItemRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.empty());

            assertThatThrownBy(() -> saleService.getDetailSale(FLOW_CODE, SaleStatus.SALE.name()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND);
        }

        @Test
        @DisplayName("PAYMENT 상태 — salePayment 정상 조회")
        void payment_정상조회() {
            SalePayment payment = mock(SalePayment.class);
            Sale parent = mock(Sale.class);
            given(payment.getFlowCode()).willReturn(FLOW_CODE);
            given(payment.getCreateDate()).willReturn(LocalDateTime.now());
            given(payment.getSaleStatus()).willReturn(SaleStatus.PAYMENT);
            given(payment.getMaterial()).willReturn("18K");
            given(payment.getPaymentNote()).willReturn("현금 결제");
            given(payment.getGoldWeight()).willReturn(new BigDecimal("3.250"));
            given(payment.getCashAmount()).willReturn(150_000);
            given(payment.getSale()).willReturn(parent);
            given(parent.getAccountId()).willReturn(STORE_ID);
            given(parent.getAccountName()).willReturn("강남금은방");
            given(parent.getAccountGrade()).willReturn("A");
            given(parent.getAccountHarry()).willReturn(new BigDecimal("1.50"));

            given(salePaymentRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(payment));

            SaleDto.Response resp = saleService.getDetailSale(FLOW_CODE, SaleStatus.PAYMENT.name());

            assertThat(resp.getFlowCode()).isEqualTo(FLOW_CODE);
            assertThat(resp.getSaleType()).isEqualTo(SaleStatus.PAYMENT.name());
            assertThat(resp.getId()).isEqualTo(STORE_ID.toString());
        }

        @Test
        @DisplayName("PAYMENT 상태 — salePayment 없음 → 예외")
        void payment_없음_예외() {
            given(salePaymentRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.empty());

            assertThatThrownBy(() -> saleService.getDetailSale(FLOW_CODE, SaleStatus.PAYMENT.name()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND);
        }
    }

    // -----------------------------------------------------------------------
    // getSale (페이징)
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getSale")
    class GetSale {

        @Test
        @DisplayName("결과 비어있어도 정상 응답 — content empty, history lookup 안 함")
        void 빈결과() {
            Pageable pageable = PageRequest.of(0, 20);
            CustomPage<SaleItemResponse.SaleItem> empty = emptyPage();

            given(customSaleRepository.findSales(any(), eq(pageable))).willReturn(empty);
            given(statusHistoryRepository.findAllByFlowCodeInOrderByCreateAtAsc(anyList()))
                    .willReturn(Collections.emptyList());

            CustomPage<SaleItemResponse.SaleItem> result =
                    saleService.getSale("다이아", "2026-05-01", "2026-05-31", "18K", pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getAllSales
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getAllSales")
    class GetAllSales {

        @Test
        @DisplayName("Condition 그대로 위임")
        void 위임검증() {
            given(customSaleRepository.findAllSales(any())).willReturn(Collections.emptyList());

            List<SaleItemResponse.SaleItem> result =
                    saleService.getAllSales("2026-05-01", "2026-05-31", "search", "18K");

            assertThat(result).isEmpty();

            ArgumentCaptor<SaleDto.Condition> captor = ArgumentCaptor.forClass(SaleDto.Condition.class);
            verify(customSaleRepository).findAllSales(captor.capture());
            assertThat(captor.getValue().getStartAt()).isEqualTo("2026-05-01");
            assertThat(captor.getValue().getEndAt()).isEqualTo("2026-05-31");
            assertThat(captor.getValue().getInput()).isEqualTo("search");
            assertThat(captor.getValue().getMaterial()).isEqualTo("18K");
        }
    }

    // -----------------------------------------------------------------------
    // getSaleStores
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getSaleStores")
    class GetSaleStores {

        @Test
        @DisplayName("startAt/endAt 만 위임")
        void 단순_위임() {
            given(customSaleRepository.findSaleStores("2026-05-01", "2026-05-31"))
                    .willReturn(Collections.emptyList());

            assertThat(saleService.getSaleStores("2026-05-01", "2026-05-31")).isEmpty();
            verify(customSaleRepository).findSaleStores("2026-05-01", "2026-05-31");
        }
    }

    // -----------------------------------------------------------------------
    // updateSale
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateSale")
    class UpdateSale {

        @Test
        @DisplayName("eventId 가 비어있으면 IllegalStateException — 멱등키 검증")
        void eventId_누락() {
            SaleDto.updateRequest dto = mock(SaleDto.updateRequest.class);

            assertThatThrownBy(() -> saleService.updateSale(TOKEN, null, FLOW_CODE, dto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("잘못된 형식");

            assertThatThrownBy(() -> saleService.updateSale(TOKEN, "", FLOW_CODE, dto))
                    .isInstanceOf(IllegalStateException.class);

            // 멱등키 누락이면 어떤 레포지토리도 호출되면 안 된다
            verifyNoInteractions(saleItemRepository, statusHistoryRepository, storeService);
        }
    }

    // -----------------------------------------------------------------------
    // createStorePayment
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("createStorePayment")
    class CreateStorePayment {

        @Test
        @DisplayName("eventId 가 비어있으면 IllegalStateException(멱등키 누락)")
        void eventId_누락() {
            SaleDto.Request req = mock(SaleDto.Request.class);

            assertThatThrownBy(() -> saleService.createStorePayment(TOKEN, "", req, true))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("멱등키");

            assertThatThrownBy(() -> saleService.createStorePayment(TOKEN, "   ", req, true))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("멱등키 중복(DataIntegrityViolation) 시 예외 삼키고 정상 종료 — log 만 남김")
        void 멱등키_중복_조용히_종료() {
            SaleDto.Request req = paymentRequest("18K", "3.250", 150_000, SaleStatus.PAYMENT.name());

            // salePaymentRepository.saveAndFlush 가 중복 키로 터지는 시나리오
            willThrow(new DataIntegrityViolationException("dup key"))
                    .given(salePaymentRepository).saveAndFlush(any(SalePayment.class));

            given(saleRepository.findLatestSaleByAccountIdAndDate(eq(STORE_ID), any(), any()))
                    .willReturn(Optional.empty());
            given(saleRepository.save(any(Sale.class))).willAnswer(inv -> inv.getArgument(0));
            given(saleRepository.countByCreateDateBetween(any(), any())).willReturn(0L);

            // 예외가 밖으로 새지 않아야 한다
            saleService.createStorePayment(TOKEN, EVENT_ID, req, true);

            // applyBalanceChange 까지는 도달하지 않음 (예외 후 catch)
            verify(storeService, never()).applyDelta(any(), any(), any(), any(), any(), any(), any(), any());
        }
    }

    // -----------------------------------------------------------------------
    // stockToSale
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("stockToSale")
    class StockToSale {

        @Test
        @DisplayName("stock 없음 → IllegalArgumentException(NOT_FOUND)")
        void stock_없음() {
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.empty());
            StockDto.stockRequest dto = mock(StockDto.stockRequest.class);

            assertThatThrownBy(() -> saleService.stockToSale(TOKEN, EVENT_ID, FLOW_CODE, dto, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND);
        }

        @Test
        @DisplayName("이미 saleItem 존재 → 멱등성: 조용히 return, 잔액 호출 없음")
        void 이미_판매전환됨() {
            Stock stock = stubStock();
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));
            given(saleItemRepository.existsByStock(stock)).willReturn(true);

            StockDto.stockRequest dto = mock(StockDto.stockRequest.class);
            saleService.stockToSale(TOKEN, EVENT_ID, FLOW_CODE, dto, true);

            verify(storeService, never()).applyDelta(any(), any(), any(), any(), any(), any(), any(), any());
            verify(saleRepository, never()).save(any());
        }

        @Test
        @DisplayName("OrderStatus 가 STOCK/NORMAL 이 아니면 IllegalStateException")
        void 잘못된_상태() {
            Stock stock = mock(Stock.class);
            given(stock.getOrderStatus()).willReturn(OrderStatus.SALE); // 이미 판매됨
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));
            given(saleItemRepository.existsByStock(stock)).willReturn(false);

            StockDto.stockRequest dto = mock(StockDto.stockRequest.class);

            assertThatThrownBy(() -> saleService.stockToSale(TOKEN, EVENT_ID, FLOW_CODE, dto, true))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("판매로 전환 불가 상태");
        }
    }

    // -----------------------------------------------------------------------
    // orderToSale
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("orderToSale")
    class OrderToSale {

        @Test
        @DisplayName("stock 없음 → IllegalArgumentException")
        void stock_없음() {
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.empty());
            StockDto.StockRegisterRequest dto = mock(StockDto.StockRegisterRequest.class);

            assertThatThrownBy(() -> saleService.orderToSale(TOKEN, EVENT_ID, FLOW_CODE, dto, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND);
        }

        @Test
        @DisplayName("이미 saleItem 존재하면 조용히 return")
        void 이미_판매전환됨() {
            Stock stock = stubStock();
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));
            given(saleItemRepository.existsByStock(stock)).willReturn(true);

            StockDto.StockRegisterRequest dto = mock(StockDto.StockRegisterRequest.class);
            saleService.orderToSale(TOKEN, EVENT_ID, FLOW_CODE, dto, false);

            verify(factoryService, never()).applyDelta(any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("OrderStatus 가 SALE 이면 IllegalStateException")
        void 잘못된_상태() {
            Stock stock = mock(Stock.class);
            given(stock.getOrderStatus()).willReturn(OrderStatus.SALE);
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));
            given(saleItemRepository.existsByStock(stock)).willReturn(false);

            StockDto.StockRegisterRequest dto = mock(StockDto.StockRegisterRequest.class);

            assertThatThrownBy(() -> saleService.orderToSale(TOKEN, EVENT_ID, FLOW_CODE, dto, false))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // -----------------------------------------------------------------------
    // cancelSale
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("cancelSale")
    class CancelSale {

        @Test
        @DisplayName("role 이 WAIT 면 NOT_ACCESS 예외 — 권한 거부")
        void 권한_없음() {
            given(jwtUtil.getRole(TOKEN)).willReturn("WAIT");

            assertThatThrownBy(() -> saleService.cancelSale(TOKEN, EVENT_ID, "SALE", "1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(ExceptionMessage.NOT_ACCESS);

            verifyNoInteractions(saleItemRepository, salePaymentRepository);
        }

        @Test
        @DisplayName("존재하지 않는 SaleStatus type → IllegalArgumentException")
        void 잘못된_type() {
            given(jwtUtil.getRole(TOKEN)).willReturn("USER");

            assertThatThrownBy(() -> saleService.cancelSale(TOKEN, EVENT_ID, "NOT_A_REAL_STATUS", "1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 판매 타입");
        }

        @Test
        @DisplayName("SALE 취소 — 이미 반품된 SaleItem 이면 예외")
        void 이미_반품된_상품() {
            given(jwtUtil.getRole(TOKEN)).willReturn("USER");

            SaleItem item = mock(SaleItem.class);
            given(item.isReturned()).willReturn(true);
            given(saleItemRepository.findByFlowCode(1L)).willReturn(Optional.of(item));

            assertThatThrownBy(() -> saleService.cancelSale(TOKEN, EVENT_ID, "SALE", "1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 반품");
        }

        @Test
        @DisplayName("PAYMENT 취소 — payment 못 찾으면 NOT_FOUND 예외")
        void payment_없음() {
            given(jwtUtil.getRole(TOKEN)).willReturn("USER");
            given(salePaymentRepository.findByFlowCode(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> saleService.cancelSale(TOKEN, EVENT_ID, "PAYMENT", "1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND);
        }

        @Test
        @DisplayName("처리 불가 SaleStatus type (예: SALE/PAYMENT_STATUSES 외) → 예외")
        void 처리불가_type() {
            // PURCHASE 는 cancelSale 의 SALE/PAYMENT 분기 모두에 안 들어감
            given(jwtUtil.getRole(TOKEN)).willReturn("USER");

            assertThatThrownBy(() -> saleService.cancelSale(TOKEN, EVENT_ID, "PURCHASE", "1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("처리할 수 없는");
        }
    }

    // -----------------------------------------------------------------------
    // findSaleProductNameAndMaterial
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("findSaleProductNameAndMaterial")
    class FindSalePast {

        @Test
        @DisplayName("applyPastSales=false → 즉시 빈 리스트 반환, 레포 안 부름")
        void 옵션꺼짐() {
            given(storeService.getStoreInfoView(STORE_ID))
                    .willReturn(new StoreView(STORE_ID, "테스트매장", "A", "1.5", "SELL", false));

            List<SaleDto.SaleDetailDto> result =
                    saleService.findSaleProductNameAndMaterial(TOKEN, STORE_ID, 1L, "18K");

            assertThat(result).isEmpty();
            verify(customSaleRepository, never()).findSalePast(any(), any(), any());
        }

        @Test
        @DisplayName("applyPastSales=true 인데 결과 없음 → 빈 리스트, stone 집계 호출 안 함")
        void 옵션켜짐_빈결과() {
            given(storeService.getStoreInfoView(STORE_ID))
                    .willReturn(new StoreView(STORE_ID, "테스트매장", "A", "1.5", "SELL", true));
            given(customSaleRepository.findSalePast(STORE_ID, 1L, "18K"))
                    .willReturn(Collections.emptyList());

            List<SaleDto.SaleDetailDto> result =
                    saleService.findSaleProductNameAndMaterial(TOKEN, STORE_ID, 1L, "18K");

            assertThat(result).isEmpty();
            verify(customOrderStoneRepository, never()).findStoneCountsByStockIds(any());
        }
    }

    // -----------------------------------------------------------------------
    // checkBeforeSale
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("checkBeforeSale")
    class CheckBeforeSale {

        @Test
        @DisplayName("당일 판매 세션 없으면 NOT_FOUND 예외")
        void 없음() {
            given(saleRepository.findSaleCodeByAccountIdAndDate(eq(STORE_ID), any(), any()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> saleService.checkBeforeSale(STORE_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND);
        }

        @Test
        @DisplayName("정상 — saleCode/displayCode/accountGoldPrice 반환")
        void 정상() {
            Sale sale = mock(Sale.class);
            given(sale.getSaleCode()).willReturn(SALE_CODE);
            given(sale.getDisplayCode()).willReturn("2605160001");
            given(sale.getAccountGoldPrice()).willReturn(85_000);
            given(saleRepository.findSaleCodeByAccountIdAndDate(eq(STORE_ID), any(), any()))
                    .willReturn(Optional.of(sale));

            SaleDto.PastSaleRequest result = saleService.checkBeforeSale(STORE_ID);

            assertThat(result.getSaleCode()).isEqualTo(SALE_CODE.toString());
            assertThat(result.getDisplayCode()).isEqualTo("2605160001");
            assertThat(result.getAccountGoldPrice()).isEqualTo(85_000);
        }
    }

    // -----------------------------------------------------------------------
    // checkAccountGoldPrice
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("checkAccountGoldPrice")
    class CheckAccountGoldPrice {

        @Test
        @DisplayName("Sale 없음 → NOT_FOUND 예외")
        void sale_없음() {
            given(saleRepository.findBySaleCode(SALE_CODE)).willReturn(Optional.empty());

            assertThatThrownBy(() -> saleService.checkAccountGoldPrice(SALE_CODE.toString()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND);
        }

        @Test
        @DisplayName("isAccountGoldPrice=true → 저장된 시세 반환")
        void 시세_있음() {
            Sale sale = mock(Sale.class);
            given(sale.isAccountGoldPrice()).willReturn(true);
            given(sale.getAccountGoldPrice()).willReturn(85_000);
            given(saleRepository.findBySaleCode(SALE_CODE)).willReturn(Optional.of(sale));

            assertThat(saleService.checkAccountGoldPrice(SALE_CODE.toString())).isEqualTo(85_000);
        }

        @Test
        @DisplayName("isAccountGoldPrice=false → 0 반환")
        void 시세_없음() {
            Sale sale = mock(Sale.class);
            given(sale.isAccountGoldPrice()).willReturn(false);
            given(saleRepository.findBySaleCode(SALE_CODE)).willReturn(Optional.of(sale));

            assertThat(saleService.checkAccountGoldPrice(SALE_CODE.toString())).isEqualTo(0);
        }
    }

    // -----------------------------------------------------------------------
    // updateAccountGoldPrice
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateAccountGoldPrice")
    class UpdateAccountGoldPrice {

        @Test
        @DisplayName("Sale 없음 → NOT_FOUND 예외")
        void sale_없음() {
            given(saleRepository.findBySaleCodeAndSalePayments(SALE_CODE)).willReturn(Optional.empty());

            SaleDto.GoldPriceRequest req = mock(SaleDto.GoldPriceRequest.class);
            assertThatThrownBy(() -> saleService.updateAccountGoldPrice(SALE_CODE.toString(), req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND);
        }

        @Test
        @DisplayName("정상 — updateAccountGoldPrice 가 entity 메서드로 호출됨")
        void 정상() {
            Sale sale = mock(Sale.class);
            given(sale.isAccountGoldPrice()).willReturn(false);
            given(saleRepository.findBySaleCodeAndSalePayments(SALE_CODE)).willReturn(Optional.of(sale));

            SaleDto.GoldPriceRequest req = mock(SaleDto.GoldPriceRequest.class);
            given(req.getAccountGoldPrice()).willReturn(90_000);

            saleService.updateAccountGoldPrice(SALE_CODE.toString(), req);

            verify(sale).updateAccountGoldPrice(90_000);
        }
    }

    // -----------------------------------------------------------------------
    // getSalePrint
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getSalePrint")
    class GetSalePrint {

        @Test
        @DisplayName("printSales 가 비어있으면 IndexOutOfBoundsException — 알려진 잠재 버그")
        void 빈결과_NPE_위험() {
            given(customSaleRepository.findPrintSales(SALE_CODE.toString()))
                    .willReturn(Collections.emptyList());

            // 현 구현이 printSales.get(0) 를 무방비로 호출하므로 IndexOutOfBoundsException 가 나는 게 정상.
            // 향후 가드 추가 시 이 테스트가 깨지면 expected 를 바꿔주면 된다.
            assertThatThrownBy(() -> saleService.getSalePrint(TOKEN, SALE_CODE.toString()))
                    .isInstanceOf(IndexOutOfBoundsException.class);
        }

        @Test
        @DisplayName("storeName 없으면 잔액 조회 안 하고 saleItemResponses 만 채워서 반환")
        void storeName_없음_분기() {
            SaleItemResponse single = mock(SaleItemResponse.class);
            given(single.getStoreName()).willReturn(null);
            given(single.getSaleItems()).willReturn(Collections.emptyList());

            given(customSaleRepository.findPrintSales(SALE_CODE.toString()))
                    .willReturn(List.of(single));
            given(productService.getProductImages(any())).willReturn(Map.of());

            SalePrintResponse resp = saleService.getSalePrint(TOKEN, SALE_CODE.toString());

            assertThat(resp).isNotNull();
            verify(storeService, never()).getReceivableLog(any(), any());
        }
    }

    // -----------------------------------------------------------------------
    // getSalesExcel
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getSalesExcel")
    class GetSalesExcel {

        @Test
        @DisplayName("빈 결과 → 헤더만 들어있는 워크북 byte[] (길이 > 0)")
        void 빈결과_헤더만() throws Exception {
            given(customSaleRepository.findSalesForExcel(any())).willReturn(Collections.emptyList());

            byte[] bytes = saleService.getSalesExcel(null, "2026-05-01", "2026-05-31", "18K");

            assertThat(bytes).isNotEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // 헬퍼
    // -----------------------------------------------------------------------
    private static Stock stubStock() {
        Stock stock = mock(Stock.class);
        ProductSnapshot product = mock(ProductSnapshot.class);
        given(stock.getStoreId()).willReturn(STORE_ID);
        given(stock.getFactoryId()).willReturn(FACTORY_ID);
        given(stock.getStoreHarry()).willReturn(new BigDecimal("1.50"));
        given(stock.getStoreGrade()).willReturn("A");
        given(stock.getOrderStatus()).willReturn(OrderStatus.STOCK);
        given(stock.getOrderStones()).willReturn(new ArrayList<>());
        given(stock.getProduct()).willReturn(product);
        given(product.getMaterialName()).willReturn("18K");
        given(product.getGoldWeight()).willReturn(new BigDecimal("3.250"));
        return stock;
    }

    private static SaleDto.Request paymentRequest(String material, String goldWeight,
                                                  Integer payAmount, String orderStatus) {
        // setter 가 없는 DTO 라 mock 으로 처리
        SaleDto.Request req = mock(SaleDto.Request.class);
        given(req.getId()).willReturn(STORE_ID);
        given(req.getName()).willReturn("강남금은방");
        given(req.getHarry()).willReturn(new BigDecimal("1.50"));
        given(req.getGrade()).willReturn("A");
        given(req.getOrderStatus()).willReturn(orderStatus);
        given(req.getMaterial()).willReturn(material);
        given(req.getGoldWeight()).willReturn(goldWeight);
        given(req.getPayAmount()).willReturn(payAmount);
        given(req.getNote()).willReturn("단위테스트 결제");
        given(req.getAccountGoldPrice()).willReturn(85_000);
        return req;
    }

    @SuppressWarnings("unchecked")
    private static CustomPage<SaleItemResponse.SaleItem> emptyPage() {
        CustomPage<SaleItemResponse.SaleItem> page = mock(CustomPage.class);
        given(page.getContent()).willReturn(Collections.emptyList());
        given(page.stream()).willReturn(java.util.stream.Stream.empty());
        return page;
    }
}
