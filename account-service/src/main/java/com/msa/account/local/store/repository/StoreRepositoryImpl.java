package com.msa.account.local.store.repository;


import com.msa.account.global.domain.dto.AccountDto;
import com.msa.account.global.domain.dto.QAccountDto_AccountResponse;
import com.msa.account.global.domain.dto.QAccountDto_AccountSaleLogResponse;
import com.msa.account.global.domain.dto.QAccountDto_AccountSingleResponse;
import com.msa.account.global.excel.dto.AccountExcelDto;
import com.msa.account.global.excel.dto.QAccountExcelDto;
import com.msa.account.global.excel.dto.ReceivableExcelDto;
import com.msa.account.global.excel.dto.QReceivableExcelDto;
import com.msa.account.local.store.domain.dto.QStoreDto_StoreResponse;
import com.msa.account.local.store.domain.dto.StoreDto;
import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import com.msa.common.global.util.CustomPage;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.msa.account.global.domain.entity.QAddress.address;
import static com.msa.account.global.domain.entity.QCommonOption.commonOption;
import static com.msa.account.global.domain.entity.QGoldHarry.goldHarry;
import static com.msa.account.local.store.domain.entity.QAdditionalOption.additionalOption;
import static com.msa.account.local.store.domain.entity.QStore.store;
import static com.msa.account.local.transaction_history.domain.entity.QTransactionHistory.transactionHistory;

/**
 * 매장 QueryDSL 쿼리 구현체.
 *
 * *매장 단건/목록 조회, 미수금 조회, 엑셀 데이터 추출 등 다양한 쿼리를 제공한다.
 *
 * *주요 특징:
 *
 *   - LEFT JOIN 4개 테이블 — {@code store} 기준으로 {@code address},
 *       {@code commonOption}, {@code additionalOption}, {@code goldHarry} 를 각각 LEFT JOIN
 *   - 미수금 서브쿼리 — {@code transactionHistory} 테이블에서
 *       최신 판매일({@code SaleStatus.SALE})과 최신 결제일({@code SaleStatus.PAYMENT})을
 *       {@code TO_CHAR(MAX(...))} 서브쿼리로 각각 조회하여 응답 DTO에 포함
 *   - 미수금 필터 — 잔액이 0이 아니거나 거래 이력이 존재하는 매장만 조회하는 EXISTS 조건 적용
 *   - 엑셀 다운로드 — 매장 목록 및 미수금 목록 각각에 대해 엑셀 전용 DTO로 데이터를 조회
 *   - 동적 정렬 — 매장명·대표자명·등급·금 잔액·현금 잔액 기준 ASC/DESC 지원
 * 
 *
 * *의존성: {@link JPAQueryFactory}, {@code store}, {@code address},
 * {@code commonOption}, {@code additionalOption}, {@code goldHarry},
 * {@code transactionHistory} Q클래스
 */
public class StoreRepositoryImpl implements CustomStoreRepository {

    private final JPAQueryFactory query;

    public StoreRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public Optional<AccountDto.AccountSingleResponse> findByStoreId(Long storeId) {
        return Optional.ofNullable(query
                .select(new QAccountDto_AccountSingleResponse(
                        store.storeId.stringValue(),
                        store.storeName,
                        store.storeOwnerName,
                        store.storePhoneNumber,
                        store.storeContactNumber1,
                        store.storeContactNumber2,
                        store.storeFaxNumber,
                        store.storeNote,

                        store.address.addressId.stringValue(),
                        store.address.addressZipCode,
                        store.address.addressBasic,
                        store.address.addressAdd,

                        store.commonOption.commonOptionId.stringValue(),
                        store.commonOption.optionTradeType.stringValue(),
                        store.commonOption.optionLevel.stringValue(),
                        store.commonOption.goldHarry.goldHarryId.stringValue(),
                        store.commonOption.goldHarry.goldHarryLoss.stringValue(),

                        store.additionalOption.optionId.stringValue(),
                        store.additionalOption.optionApplyPastSales,
                        store.additionalOption.optionMaterialId.stringValue(),
                        store.additionalOption.optionMaterialName))
                .from(store)
                .leftJoin(store.address, address)
                .leftJoin(store.commonOption, commonOption)
                .leftJoin(store.additionalOption, additionalOption)
                .leftJoin(store.commonOption.goldHarry, goldHarry)
                .where(store.storeId.eq(storeId).and(store.storeDeleted.isFalse()))
                .fetchOne());
    }

    @Override
    public CustomPage<StoreDto.StoreResponse> findAllStore(String name, String searchField, String sortField, String sortOrder, Pageable pageable) {

        BooleanExpression searchCondition = buildStoreSearchCondition(name, searchField);
        OrderSpecifier<?>[] orderSpecifiers = specifiers(sortField, sortOrder);

        List<StoreDto.StoreResponse> content = query
                .select(new QStoreDto_StoreResponse(
                        store.storeId,
                        store.storeName,
                        store.storeOwnerName,
                        store.storePhoneNumber,
                        store.storeContactNumber1,
                        store.storeContactNumber2,
                        store.storeFaxNumber,
                        store.storeNote,
                        Expressions.stringTemplate(
                                "concat({0}, ' ', {1}, ' ', {2})",
                                store.address.addressZipCode,
                                store.address.addressBasic,
                                store.address.addressAdd
                        ),
                        store.commonOption.optionTradeType.stringValue(),
                        store.commonOption.optionLevel.stringValue(),
                        store.commonOption.goldHarryLoss))
                .from(store)
                .leftJoin(store.address, address)
                .leftJoin(store.commonOption, commonOption)
                .where(store.storeDeleted.isFalse().and(searchCondition))
                .orderBy(orderSpecifiers)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = query
                .select(store.count())
                .from(store)
                .leftJoin(store.address, address)
                .leftJoin(store.commonOption, commonOption)
                .where(store.storeDeleted.isFalse().and(searchCondition));

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }

    /**
     * 검색 필드에 따라 동적으로 BooleanExpression을 구성한다.
     * searchField 미지정 시 storeName(판매처명) 기본 검색.
     */
    private BooleanExpression buildStoreSearchCondition(String name, String searchField) {
        if (!StringUtils.hasText(name)) {
            return null;
        }

        if (!StringUtils.hasText(searchField)) {
            return store.storeName.contains(name);
        }

        return switch (searchField) {
            case "accountName", "storeName" -> store.storeName.contains(name);
            case "accountOwnerName", "ownerName" -> store.storeOwnerName.contains(name);
            case "phoneNumber" -> store.storePhoneNumber.contains(name);
            case "faxNumber" -> store.storeFaxNumber.contains(name);
            case "businessNumber1" -> store.storeContactNumber1.contains(name);
            case "businessNumber2" -> store.storeContactNumber2.contains(name);
            case "note" -> store.storeNote.contains(name);
            case "grade" -> store.commonOption.optionLevel.stringValue().contains(name);
            default -> store.storeName.contains(name);
        };
    }

    @Override
    public CustomPage<AccountDto.AccountResponse> findAllStoreAndReceivable(String name, String field, String sort, Pageable pageable) {
        BooleanExpression storeName = name != null ? store.storeName.contains(name) : null;

        StringExpression latestTransactionDateString = Expressions.stringTemplate(
                "TO_CHAR(MAX(transactionHistory.transactionDate), 'YYYY-MM-DD HH24:MI:SS')",
                transactionHistory.transactionDate.max()
        );

        JPQLQuery<String> lastPaymentDateQuery = JPAExpressions
                .select(latestTransactionDateString)
                .from(transactionHistory)
                .where(transactionHistory.store.eq(store)
                        .and(transactionHistory.transactionDeleted.isFalse())
                        .and(transactionHistory.transactionType.eq(SaleStatus.PAYMENT)));

        JPQLQuery<String> lastSaleDateQuery = JPAExpressions
                .select(latestTransactionDateString)
                .from(transactionHistory)
                .where(transactionHistory.store.eq(store)
                        .and(transactionHistory.transactionDeleted.isFalse())
                        .and(transactionHistory.transactionType.eq(SaleStatus.SALE)));

        BooleanExpression hasHistory = JPAExpressions
                .selectOne()
                .from(transactionHistory)
                .where(transactionHistory.store.eq(store)
                        .and(transactionHistory.transactionDeleted.isFalse())
                        .or(transactionHistory.transactionType.eq(SaleStatus.PAYMENT))
                        .or(transactionHistory.transactionType.eq(SaleStatus.SALE)))
                .exists();

        OrderSpecifier<?>[] specifiers = specifiers(field, sort);

        List<AccountDto.AccountResponse> content = query
                .select(new QAccountDto_AccountResponse(
                        store.storeId,
                        store.storeName,
                        store.currentGoldBalance.stringValue(),
                        store.currentMoneyBalance.stringValue(),
                        lastSaleDateQuery,
                        store.storeOwnerName,
                        store.storePhoneNumber,
                        store.storeContactNumber1,
                        store.storeContactNumber2,
                        store.storeFaxNumber,
                        store.storeNote,
                        store.commonOption.optionLevel.stringValue(),
                        store.commonOption.optionTradeType.stringValue(),
                        store.commonOption.goldHarryLoss,
                        lastPaymentDateQuery,
                        Expressions.stringTemplate(
                                "concat({0}, ' ', {1}, ' ', {2})",
                                store.address.addressZipCode,
                                store.address.addressBasic,
                                store.address.addressAdd
                        )))
                .from(store)
                .leftJoin(store.address, address)
                .leftJoin(store.commonOption, commonOption)
                .where(
                        store.storeDeleted.isFalse().and(storeName),
                        store.currentGoldBalance.ne(BigDecimal.ZERO).or(store.currentMoneyBalance.ne(0L)),
                        hasHistory
                )
                .orderBy(specifiers)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = query
                .select(store.count())
                .from(store)
                .where(
                        store.storeDeleted.isFalse().and(storeName),
                        store.currentGoldBalance.ne(BigDecimal.ZERO).or(store.currentMoneyBalance.ne(0L)),
                        hasHistory
                );

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }

    @Override
    public AccountDto.AccountResponse findByStoreIdAndReceivable(Long storeId) {
        BooleanExpression isStore = storeId != null ? store.storeId.eq(storeId) : null;

        StringExpression latestTransactionDateString = Expressions.stringTemplate(
                "TO_CHAR(MAX(transactionHistory.transactionDate), 'YYYY-MM-DD HH24:MI:SS')",
                transactionHistory.transactionDate.max()
        );

        JPQLQuery<String> lastPaymentDateQuery = JPAExpressions
                .select(latestTransactionDateString)
                .from(transactionHistory)
                .where(transactionHistory.store.eq(store)
                        .and(transactionHistory.transactionDeleted.isFalse())
                        .and(transactionHistory.transactionType.eq(SaleStatus.PAYMENT)));

        JPQLQuery<String> lastSaleDateQuery = JPAExpressions
                .select(latestTransactionDateString)
                .from(transactionHistory)
                .where(transactionHistory.store.eq(store)
                        .and(transactionHistory.transactionDeleted.isFalse())
                        .and(transactionHistory.transactionType.eq(SaleStatus.SALE)));


        return query
                .select(new QAccountDto_AccountResponse(
                        store.storeId,
                        store.storeName,
                        store.currentGoldBalance.stringValue(),
                        store.currentMoneyBalance.stringValue(),
                        lastSaleDateQuery,
                        store.storeOwnerName,
                        store.storePhoneNumber,
                        store.storeContactNumber1,
                        store.storeContactNumber2,
                        store.storeFaxNumber,
                        store.storeNote,
                        store.commonOption.optionLevel.stringValue(),
                        store.commonOption.optionTradeType.stringValue(),
                        store.commonOption.goldHarryLoss,
                        lastPaymentDateQuery,
                        Expressions.stringTemplate(
                                "concat({0}, ' ', {1}, ' ', {2})",
                                store.address.addressZipCode,
                                store.address.addressBasic,
                                store.address.addressAdd
                        )))
                .from(store)
                .leftJoin(store.address, address)
                .leftJoin(store.commonOption, commonOption)
                .where(store.storeDeleted.isFalse().and(isStore))
                .fetchOne();
    }

    @Override
    public AccountDto.AccountSaleLogResponse findByStoreIdAndReceivableByLog(Long storeId) {
        BooleanExpression isStore = storeId != null ? store.storeId.eq(storeId) : null;

        StringExpression latestTransactionDateString = Expressions.stringTemplate(
                "TO_CHAR(MAX(transactionHistory.transactionDate), 'YYYY-MM-DD HH24:MI:SS')",
                transactionHistory.transactionDate.max()
        );

        JPQLQuery<String> lastPaymentDateQuery = JPAExpressions
                .select(latestTransactionDateString)
                .from(transactionHistory)
                .where(transactionHistory.store.eq(store)
                        .and(transactionHistory.transactionDeleted.isFalse())
                        .and(transactionHistory.transactionType.eq(SaleStatus.PAYMENT)));

        JPQLQuery<String> lastSaleDateQuery = JPAExpressions
                .select(latestTransactionDateString)
                .from(transactionHistory)
                .where(transactionHistory.store.eq(store)
                        .and(transactionHistory.transactionDeleted.isFalse())
                        .and(transactionHistory.transactionType.eq(SaleStatus.SALE)));


        return query
                .select(new QAccountDto_AccountSaleLogResponse(
                        store.storeId,
                        store.storeName,
                        lastSaleDateQuery,
                        store.storeOwnerName,
                        store.storePhoneNumber,
                        store.storeContactNumber1,
                        store.storeContactNumber2,
                        store.storeFaxNumber,
                        store.storeNote,
                        store.commonOption.optionLevel.stringValue(),
                        store.commonOption.optionTradeType.stringValue(),
                        store.commonOption.goldHarryLoss,
                        lastPaymentDateQuery,
                        Expressions.stringTemplate(
                                "concat({0}, ' ', {1}, ' ', {2})",
                                store.address.addressZipCode,
                                store.address.addressBasic,
                                store.address.addressAdd
                        )))
                .from(store)
                .leftJoin(store.address, address)
                .leftJoin(store.commonOption, commonOption)
                .where(store.storeDeleted.isFalse().and(isStore))
                .fetchOne();
    }

    @Override
    public List<AccountExcelDto> findAllStoreExcel() {
        return query
                .select(new QAccountExcelDto(
                        Expressions.constant("상점"),
                        store.createDate.stringValue(),
                        store.createdBy,
                        store.storeName,
                        store.storeOwnerName,
                        store.storePhoneNumber,
                        store.storeContactNumber1,
                        store.storeContactNumber2,
                        store.storeFaxNumber,
                        store.storeNote,
                        Expressions.stringTemplate(
                                "concat({0}, ' ', {1}, ' ', {2})",
                                store.address.addressZipCode,
                                store.address.addressBasic,
                                store.address.addressAdd
                        ),
                        store.commonOption.optionTradeType.stringValue(),
                        store.commonOption.optionLevel.stringValue(),
                        store.commonOption.goldHarryLoss
                ))
                .from(store)
                .leftJoin(store.address, address)
                .leftJoin(store.commonOption, commonOption)
                .orderBy(store.storeName.desc())
                .fetch();
    }

    @Override
    public List<ReceivableExcelDto> findAllReceivableExcel(String name) {
        BooleanExpression storeName = name != null ? store.storeName.contains(name) : null;

        StringExpression latestTransactionDateString = Expressions.stringTemplate(
                "TO_CHAR(MAX(transactionHistory.transactionDate), 'YYYY-MM-DD HH24:MI:SS')",
                transactionHistory.transactionDate.max()
        );

        JPQLQuery<String> lastPaymentDateQuery = JPAExpressions
                .select(latestTransactionDateString)
                .from(transactionHistory)
                .where(transactionHistory.store.eq(store)
                        .and(transactionHistory.transactionDeleted.isFalse())
                        .and(transactionHistory.transactionType.eq(SaleStatus.PAYMENT)));

        JPQLQuery<String> lastSaleDateQuery = JPAExpressions
                .select(latestTransactionDateString)
                .from(transactionHistory)
                .where(transactionHistory.store.eq(store)
                        .and(transactionHistory.transactionDeleted.isFalse())
                        .and(transactionHistory.transactionType.eq(SaleStatus.SALE)));

        BooleanExpression hasHistory = JPAExpressions
                .selectOne()
                .from(transactionHistory)
                .where(transactionHistory.store.eq(store)
                        .and(transactionHistory.transactionDeleted.isFalse())
                        .or(transactionHistory.transactionType.eq(SaleStatus.PAYMENT))
                        .or(transactionHistory.transactionType.eq(SaleStatus.SALE)))
                .exists();

        return query
                .select(new QReceivableExcelDto(
                        store.storeId,
                        store.storeName,
                        store.commonOption.optionLevel.stringValue(),
                        store.currentGoldBalance.stringValue(),
                        store.currentMoneyBalance.stringValue(),
                        lastSaleDateQuery,
                        lastPaymentDateQuery,
                        store.storeNote
                ))
                .from(store)
                .leftJoin(store.address, address)
                .leftJoin(store.commonOption, commonOption)
                .where(
                        store.storeDeleted.isFalse().and(storeName),
                        store.currentGoldBalance.ne(BigDecimal.ZERO).or(store.currentMoneyBalance.ne(0L)),
                        hasHistory
                )
                .orderBy(store.storeName.desc())
                .fetch();
    }

    private OrderSpecifier<?>[] specifiers(String sortField, String sortType) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        if (StringUtils.hasText(sortField)) {
            Order direction = "ASC".equalsIgnoreCase(sortType) ? Order.ASC : Order.DESC;

            switch (sortField) {
                case "accountName", "storeName" -> orderSpecifiers.add(new OrderSpecifier<>(direction, store.storeName));
                case "accountOwnerName", "ownerName" -> orderSpecifiers.add(new OrderSpecifier<>(direction, store.storeOwnerName));
                case "grade" -> orderSpecifiers.add(new OrderSpecifier<>(direction, store.commonOption.optionLevel));
                case "gold" -> orderSpecifiers.add(new OrderSpecifier<>(direction, store.currentGoldBalance));
                case "money" -> orderSpecifiers.add(new OrderSpecifier<>(direction, store.currentMoneyBalance));
                case "createDate" -> orderSpecifiers.add(new OrderSpecifier<>(direction, store.createDate));

                default -> {
                    orderSpecifiers.add(new OrderSpecifier<>(Order.DESC, store.storeName));
                }
            }
        } else {
            orderSpecifiers.add(new OrderSpecifier<>(Order.DESC, store.storeName));
        }

        return orderSpecifiers.toArray(new OrderSpecifier[0]);
    }

}
