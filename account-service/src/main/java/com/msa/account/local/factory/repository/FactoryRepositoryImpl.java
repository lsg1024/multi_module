package com.msa.account.local.factory.repository;

import com.msa.account.global.domain.dto.AccountDto;
import com.msa.account.global.domain.dto.QAccountDto_AccountSingleResponse;
import com.msa.account.global.excel.dto.AccountExcelDto;
import com.msa.account.global.excel.dto.QAccountExcelDto;
import com.msa.account.local.factory.domain.dto.FactoryDto;
import com.msa.account.local.factory.domain.dto.QFactoryDto_FactoryResponse;
import com.msa.common.global.util.CustomPage;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static com.msa.account.global.domain.entity.QAddress.address;
import static com.msa.account.global.domain.entity.QCommonOption.commonOption;
import static com.msa.account.global.domain.entity.QGoldHarry.goldHarry;
import static com.msa.account.local.factory.domain.entity.QFactory.factory;

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
    public CustomPage<FactoryDto.FactoryResponse> findAllFactory(String name, Pageable pageable) {

        BooleanExpression factoryName = name != null ? factory.factoryName.contains(name) : null;

        List<FactoryDto.FactoryResponse> content = query
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
                .where(factory.factoryDeleted.isFalse().and(factoryName))
                .orderBy(factory.factoryName.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = query
                .select(factory.count())
                .from(factory)
                .where(factory.factoryDeleted.isFalse().and(factoryName));

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
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
}
