package com.msa.account.local.store.repository;


import com.msa.account.global.domain.dto.AccountDto;
import com.msa.account.global.domain.dto.QAccountDto_AccountSingleResponse;
import com.msa.account.global.domain.dto.QAccountDto_accountResponse;
import com.msa.account.global.excel.dto.AccountExcelDto;
import com.msa.account.global.excel.dto.QAccountExcelDto;
import com.msa.account.local.store.domain.dto.QStoreDto_StoreResponse;
import com.msa.account.local.store.domain.dto.StoreDto;
import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import com.msa.common.global.util.CustomPage;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static com.msa.account.global.domain.entity.QAddress.address;
import static com.msa.account.global.domain.entity.QCommonOption.commonOption;
import static com.msa.account.global.domain.entity.QGoldHarry.goldHarry;
import static com.msa.account.local.store.domain.entity.QAdditionalOption.additionalOption;
import static com.msa.account.local.store.domain.entity.QStore.store;
import static com.msa.account.local.transaction_history.domain.entity.QTransactionHistory.transactionHistory;

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
    public CustomPage<StoreDto.StoreResponse> findAllStore(String name, Pageable pageable) {

        BooleanExpression storeName = name != null ? store.storeName.contains(name) : null;

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
                .where(store.storeDeleted.isFalse().and(storeName))
                .orderBy(store.storeName.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = query
                .select(store.count())
                .from(store)
                .where(store.storeDeleted.isFalse().and(storeName));

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }

    @Override
    public CustomPage<AccountDto.accountResponse> findAllStoreAndAttempt(String name, Pageable pageable) {
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
                        .and(transactionHistory.transactionType.eq(SaleStatus.PAYMENT.name())));

        JPQLQuery<String> lastSaleDateQuery = JPAExpressions
                .select(latestTransactionDateString)
                .from(transactionHistory)
                .where(transactionHistory.store.eq(store)
                        .and(transactionHistory.transactionDeleted.isFalse())
                        .and(transactionHistory.transactionType.eq(SaleStatus.SALE.name())));

        BooleanExpression hasHistory = JPAExpressions
                .selectOne()
                .from(transactionHistory)
                .where(transactionHistory.store.eq(store)
                        .and(transactionHistory.transactionDeleted.isFalse())
                        .or(transactionHistory.transactionType.eq(SaleStatus.PAYMENT.name()))
                        .or(transactionHistory.transactionType.eq(SaleStatus.SALE.name())))
                .exists();

        List<AccountDto.accountResponse> content = query
                .select(new QAccountDto_accountResponse(
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
                        hasHistory
                )
                .orderBy(store.storeName.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = query
                .select(store.count())
                .from(store)
                .where(
                        store.storeDeleted.isFalse().and(storeName),
                        hasHistory
                );

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }

    @Override
    public AccountDto.accountResponse findByStoreIdAndAttempt(Long flowCode) {
        BooleanExpression isStore = flowCode != null ? store.storeId.eq(flowCode) : null;

        StringExpression latestTransactionDateString = Expressions.stringTemplate(
                "TO_CHAR(MAX(transactionHistory.transactionDate), 'YYYY-MM-DD HH24:MI:SS')",
                transactionHistory.transactionDate.max()
        );

        JPQLQuery<String> lastPaymentDateQuery = JPAExpressions
                .select(latestTransactionDateString)
                .from(transactionHistory)
                .where(transactionHistory.store.eq(store)
                        .and(transactionHistory.transactionDeleted.isFalse())
                        .and(transactionHistory.transactionType.eq(SaleStatus.PAYMENT.name())));

        JPQLQuery<String> lastSaleDateQuery = JPAExpressions
                .select(latestTransactionDateString)
                .from(transactionHistory)
                .where(transactionHistory.store.eq(store)
                        .and(transactionHistory.transactionDeleted.isFalse())
                        .and(transactionHistory.transactionType.eq(SaleStatus.SALE.name())));


        return query
                .select(new QAccountDto_accountResponse(
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


}
