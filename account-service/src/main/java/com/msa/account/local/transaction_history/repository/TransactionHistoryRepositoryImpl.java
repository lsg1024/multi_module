package com.msa.account.local.transaction_history.repository;

import com.msa.account.local.transaction_history.domain.dto.QTransactionPage;
import com.msa.account.local.transaction_history.domain.dto.TransactionPage;
import com.msa.common.global.util.CustomPage;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Coalesce;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.msa.account.local.factory.domain.entity.QFactory.factory;
import static com.msa.account.local.store.domain.entity.QStore.store;
import static com.msa.account.local.transaction_history.domain.entity.QTransactionHistory.transactionHistory;

public class TransactionHistoryRepositoryImpl implements CustomTransactionHistoryRepository{

    private final JPAQueryFactory query;

    public TransactionHistoryRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }


    @Override
    public CustomPage<TransactionPage> findTransactionHistory(String start, String end, String accountType, String accountName, Pageable pageable) {

        List<TransactionPage> content = query
                .select(new QTransactionPage(
                        transactionHistory.eventId,
                        new Coalesce<>(String.class)
                                .add(store.storeName)
                                .add(factory.factoryName)
                                .as("accountName"),
                        transactionHistory.transactionDate.stringValue(),
                        transactionHistory.goldAmount.stringValue(),
                        transactionHistory.moneyAmount.stringValue(),
                        transactionHistory.transactionType.stringValue()
                ))
                .from(transactionHistory)
                .leftJoin(transactionHistory.store, store)
                .leftJoin(transactionHistory.factory, factory)
                .where(
                        transactionHistory.transactionDeleted.isFalse(),
                        dateBetween(start, end),
                        accountTypeEq(accountType),
                        accountNameEq(accountName)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(transactionHistory.transactionId.desc())
                .fetch();

        JPAQuery<Long> countQuery = query
                .select(transactionHistory.count())
                .from(transactionHistory)
                .where(
                        transactionHistory.transactionDeleted.isFalse(),
                        dateBetween(start, end),
                        accountTypeEq(accountType),
                        accountNameEq(accountName)
                );

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }

    @Override
    public CustomPage<TransactionPage> findTransactionHistoryFactory(String start, String end, String accountType, String accountName, Pageable pageable) {
        List<TransactionPage> content = query
                .select(new QTransactionPage(
                        transactionHistory.eventId,
                        transactionHistory.factory.factoryName,
                        transactionHistory.transactionDate.stringValue(),
                        transactionHistory.goldAmount.stringValue(),
                        transactionHistory.moneyAmount.stringValue(),
                        transactionHistory.transactionType.stringValue()
                ))
                .from(transactionHistory)
                .join(transactionHistory.factory, factory)
                .where(
                        transactionHistory.transactionDeleted.isFalse(),
                        dateBetween(start, end),
                        accountTypeEq(accountType),
                        accountNameEq(accountName)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(transactionHistory.transactionId.desc())
                .fetch();

        JPAQuery<Long> countQuery = query
                .select(transactionHistory.count())
                .from(transactionHistory)
                .where(
                        transactionHistory.transactionDeleted.isFalse(),
                        dateBetween(start, end),
                        accountTypeEq(accountType),
                        accountNameEq(accountName)
                );

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }

    private BooleanExpression dateBetween(String start, String end) {

        if (!StringUtils.hasText(start) && !StringUtils.hasText(end)) {
            return null;
        }

        LocalDateTime startAt = null;
        LocalDateTime endAt = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            if (StringUtils.hasText(start)) {
                startAt = LocalDate.parse(start, formatter).atStartOfDay();
            }
            if (StringUtils.hasText(end)) {
                endAt = LocalDate.parse(end, formatter).atTime(23, 59, 59);
            }
        } catch (Exception e) {
            return null;
        }

        if (startAt != null && endAt != null) {
            return transactionHistory.transactionDate.between(startAt, endAt);
        } else if (startAt != null) {
            return transactionHistory.transactionDate.goe(startAt);
        } else {
            return transactionHistory.transactionDate.loe(endAt);
        }
    }

    private BooleanExpression accountTypeEq(String accountType) {
        return StringUtils.hasText(accountType) ? transactionHistory.transactionType.eq(accountType) : null;
    }

    private BooleanExpression accountNameEq(String accountName) {
        if (!StringUtils.hasText(accountName)) {
            return null;
        }
        return store.storeName.eq(accountName).or(factory.factoryName.contains(accountName));
    }

}
