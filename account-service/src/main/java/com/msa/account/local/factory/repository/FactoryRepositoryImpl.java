package com.msa.account.local.factory.repository;

import com.msa.account.global.domain.dto.AccountDto;
import com.msa.account.global.domain.dto.QAccountDto_AccountResponse;
import com.msa.account.global.domain.dto.QAccountDto_AccountSingleResponse;
import com.msa.account.global.excel.dto.AccountExcelDto;
import com.msa.account.global.excel.dto.PurchaseExcelDto;
import com.msa.account.global.excel.dto.QAccountExcelDto;
import com.msa.account.global.excel.dto.QPurchaseExcelDto;
import com.msa.account.local.factory.domain.dto.FactoryDto;
import com.msa.account.local.factory.domain.dto.QFactoryDto_ApiFactoryInfo;
import com.msa.account.local.factory.domain.dto.QFactoryDto_FactoryResponse;
import com.msa.common.global.util.CustomPage;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.msa.account.global.domain.entity.QAddress.address;
import static com.msa.account.global.domain.entity.QCommonOption.commonOption;
import static com.msa.account.global.domain.entity.QGoldHarry.goldHarry;
import static com.msa.account.local.factory.domain.entity.QFactory.factory;
import static com.msa.account.local.transaction_history.domain.entity.QSaleLog.saleLog;

public class FactoryRepositoryImpl implements CustomFactoryRepository {

    private final JPAQueryFactory query;

    public FactoryRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public Optional<AccountDto.AccountSingleResponse> findByFactoryId(Long factoryId) {

        return Optional.ofNullable(query
                .select(new QAccountDto_AccountSingleResponse(
                        factory.factoryId.stringValue(),
                        factory.factoryName,
                        factory.factoryOwnerName,
                        factory.factoryPhoneNumber,
                        factory.factoryContactNumber1,
                        factory.factoryContactNumber2,
                        factory.factoryFaxNumber,
                        factory.factoryNote,
                        factory.address.addressId.stringValue(),
                        factory.address.addressZipCode,
                        factory.address.addressBasic,
                        factory.address.addressAdd,
                        factory.commonOption.commonOptionId.stringValue(),
                        factory.commonOption.optionTradeType.stringValue(),
                        factory.commonOption.optionLevel.stringValue(),
                        factory.commonOption.goldHarry.goldHarryId.stringValue(),
                        factory.commonOption.goldHarry.goldHarryLoss.stringValue()
                ))
                .from(factory)
                .join(factory.address, address)
                .join(factory.commonOption, commonOption)
                .join(factory.commonOption.goldHarry, goldHarry)
                .where(factory.factoryId.eq(factoryId).and(factory.factoryDeleted.isFalse()))
                .fetchOne());
    }

    @Override
    public CustomPage<FactoryDto.FactoryResponse> findAllFactory(String name, String searchField, String sortField, String sortOrder, Pageable pageable) {

        BooleanExpression searchCondition = buildFactorySearchCondition(name, searchField);
        OrderSpecifier<?>[] orderSpecifiers = factorySpecifiers(sortField, sortOrder);

        JPAQuery<FactoryDto.FactoryResponse> jpaQuery = query
                .select(new QFactoryDto_FactoryResponse(
                        factory.factoryId,
                        factory.factoryName,
                        factory.factoryOwnerName,
                        factory.factoryPhoneNumber,
                        factory.factoryContactNumber1,
                        factory.factoryContactNumber2,
                        factory.factoryFaxNumber,
                        factory.factoryNote,
                        Expressions.stringTemplate(
                                "concat({0}, ' ', {1}, ' ', {2})",
                                factory.address.addressZipCode,
                                factory.address.addressBasic,
                                factory.address.addressAdd
                        ),
                        factory.commonOption.optionTradeType.stringValue(),
                        factory.commonOption.optionLevel.stringValue(),
                        factory.commonOption.goldHarryLoss
                ))
                .from(factory)
                .join(factory.address, address)
                .join(factory.commonOption, commonOption)
                .where(factory.factoryDeleted.isFalse().and(searchCondition))
                .orderBy(orderSpecifiers);

        if (pageable.isPaged()) {
            jpaQuery.offset(pageable.getOffset())
                    .limit(pageable.getPageSize());
        }

        List<FactoryDto.FactoryResponse> content = jpaQuery.fetch();

        JPAQuery<Long> countQuery = query
                .select(factory.count())
                .from(factory)
                .join(factory.commonOption, commonOption)
                .where(factory.factoryDeleted.isFalse().and(searchCondition));

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }

    /**
     * 매입처(Factory) 검색 필드에 따라 동적으로 BooleanExpression을 구성한다.
     * searchField 미지정 시 factoryName(제조사명) 기본 검색.
     */
    private BooleanExpression buildFactorySearchCondition(String name, String searchField) {
        if (!StringUtils.hasText(name)) {
            return null;
        }

        if (!StringUtils.hasText(searchField)) {
            return factory.factoryName.contains(name);
        }

        return switch (searchField) {
            case "factoryName", "accountName" -> factory.factoryName.contains(name);
            case "factoryOwnerName", "ownerName", "accountOwnerName" -> factory.factoryOwnerName.contains(name);
            case "factoryPhoneNumber", "phoneNumber" -> factory.factoryPhoneNumber.contains(name);
            case "factoryFaxNumber", "faxNumber" -> factory.factoryFaxNumber.contains(name);
            case "factoryContactNumber1" -> factory.factoryContactNumber1.contains(name);
            case "factoryContactNumber2" -> factory.factoryContactNumber2.contains(name);
            case "factoryNote", "note" -> factory.factoryNote.contains(name);
            case "grade" -> factory.commonOption.optionLevel.stringValue().contains(name);
            default -> factory.factoryName.contains(name);
        };
    }

    /**
     * 매입처(Factory) 정렬 조건을 구성한다.
     */
    private OrderSpecifier<?>[] factorySpecifiers(String sortField, String sortType) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        if (StringUtils.hasText(sortField)) {
            Order direction = "ASC".equalsIgnoreCase(sortType) ? Order.ASC : Order.DESC;

            switch (sortField) {
                case "factoryName", "accountName" -> orderSpecifiers.add(new OrderSpecifier<>(direction, factory.factoryName));
                case "factoryOwnerName", "ownerName", "accountOwnerName" -> orderSpecifiers.add(new OrderSpecifier<>(direction, factory.factoryOwnerName));
                case "grade" -> orderSpecifiers.add(new OrderSpecifier<>(direction, factory.commonOption.optionLevel));
                case "gold" -> orderSpecifiers.add(new OrderSpecifier<>(direction, factory.currentGoldBalance));
                case "money" -> orderSpecifiers.add(new OrderSpecifier<>(direction, factory.currentMoneyBalance));
                case "createDate" -> orderSpecifiers.add(new OrderSpecifier<>(direction, factory.createDate));
                default -> orderSpecifiers.add(new OrderSpecifier<>(Order.DESC, factory.factoryName));
            }
        } else {
            orderSpecifiers.add(new OrderSpecifier<>(Order.DESC, factory.factoryName));
        }

        return orderSpecifiers.toArray(new OrderSpecifier[0]);
    }

    @Override
    public List<AccountExcelDto> findAllFactoryExcel() {
        return query
                .select(new QAccountExcelDto(
                        Expressions.constant("매입처"),
                        factory.createDate.stringValue(),
                        factory.createdBy,
                        factory.factoryName,
                        factory.factoryOwnerName,
                        factory.factoryPhoneNumber,
                        factory.factoryContactNumber1,
                        factory.factoryContactNumber2,
                        factory.factoryFaxNumber,
                        factory.factoryNote,
                        Expressions.stringTemplate(
                                "concat({0}, ' ', {1}, ' ', {2})",
                                factory.address.addressZipCode,
                                factory.address.addressBasic,
                                factory.address.addressAdd
                        ),
                        factory.commonOption.optionTradeType.stringValue(),
                        factory.commonOption.optionLevel.stringValue(),
                        factory.commonOption.goldHarryLoss
                        ))
                .from(factory)
                .leftJoin(factory.address, address)
                .leftJoin(factory.commonOption, commonOption)
                .orderBy(factory.factoryName.desc())
                .fetch();
    }

    @Override
    public List<FactoryDto.ApiFactoryInfo> findAllFactory() {
        return query
                .select(new QFactoryDto_ApiFactoryInfo(
                        factory.factoryId,
                        factory.factoryName.toUpperCase()
                ))
                .from(factory)
                .fetch();
    }

    @Override
    public CustomPage<AccountDto.AccountResponse> findAllFactoryAndPurchase(String endAt, Pageable pageable) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime endDateTime = LocalDate.parse(endAt, formatter).atTime(23, 59, 59);

        JPQLQuery<Long> maxIdSubQuery = JPAExpressions
                .select(saleLog.id.max())
                .from(saleLog)
                .where(saleLog.factory.eq(factory)
                        .and(saleLog.saleDate.loe(endDateTime)));

        Expression<String> goldBalanceSubQuery = ExpressionUtils.as(
                JPAExpressions.select(saleLog.afterGoldBalance.coalesce(BigDecimal.ZERO).stringValue())
                        .from(saleLog)
                        .where(saleLog.id.eq(maxIdSubQuery))
                        .orderBy(saleLog.saleDate.desc(), saleLog.id.desc()),
                "currentGoldBalance"
        );

        Expression<String> moneyBalanceSubQuery = ExpressionUtils.as(
                JPAExpressions.select(saleLog.afterMoneyBalance.coalesce(0L).stringValue())
                        .from(saleLog)
                        .where(saleLog.id.eq(maxIdSubQuery))
                        .orderBy(saleLog.saleDate.desc(), saleLog.id.desc()),
                "currentMoneyBalance"
        );

        List<AccountDto.AccountResponse> content = query
                .select(new QAccountDto_AccountResponse(
                        factory.factoryId,
                        factory.factoryName,
                        goldBalanceSubQuery,
                        moneyBalanceSubQuery
                ))
                .from(factory)
                .where(
                        factory.factoryDeleted.isFalse(),
                        factory.currentGoldBalance.ne(BigDecimal.ZERO).or(factory.currentMoneyBalance.ne(0L))
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = query
                .select(factory.count())
                .from(factory)
                .where(
                        factory.factoryDeleted.isFalse(),
                        factory.currentGoldBalance.ne(BigDecimal.ZERO).or(factory.currentMoneyBalance.ne(0L))
                );

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }

    @Override
    public List<PurchaseExcelDto> findAllPurchaseExcel(String endAt) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime endDateTime = LocalDate.parse(endAt, formatter).atTime(23, 59, 59);

        JPQLQuery<Long> maxIdSubQuery = JPAExpressions
                .select(saleLog.id.max())
                .from(saleLog)
                .where(saleLog.factory.eq(factory)
                        .and(saleLog.saleDate.loe(endDateTime)));

        Expression<String> goldBalanceSubQuery = ExpressionUtils.as(
                JPAExpressions.select(saleLog.afterGoldBalance.coalesce(BigDecimal.ZERO).stringValue())
                        .from(saleLog)
                        .where(saleLog.id.eq(maxIdSubQuery))
                        .orderBy(saleLog.saleDate.desc(), saleLog.id.desc()),
                "currentGoldBalance"
        );

        Expression<String> moneyBalanceSubQuery = ExpressionUtils.as(
                JPAExpressions.select(saleLog.afterMoneyBalance.coalesce(0L).stringValue())
                        .from(saleLog)
                        .where(saleLog.id.eq(maxIdSubQuery))
                        .orderBy(saleLog.saleDate.desc(), saleLog.id.desc()),
                "currentMoneyBalance"
        );

        return query
                .select(new QPurchaseExcelDto(
                        factory.factoryId,
                        factory.factoryName,
                        factory.commonOption.optionLevel.stringValue(),
                        goldBalanceSubQuery,
                        moneyBalanceSubQuery,
                        factory.factoryNote
                ))
                .from(factory)
                .leftJoin(factory.commonOption, commonOption)
                .where(
                        factory.factoryDeleted.isFalse(),
                        factory.currentGoldBalance.ne(BigDecimal.ZERO).or(factory.currentMoneyBalance.ne(0L))
                )
                .orderBy(factory.factoryName.desc())
                .fetch();
    }

}
