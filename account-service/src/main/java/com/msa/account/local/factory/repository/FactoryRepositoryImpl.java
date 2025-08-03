package com.msa.account.domain.factory.repository;

import com.msa.account.domain.factory.dto.FactoryDto;
import com.msa.account.domain.factory.dto.QFactoryDto_FactoryResponse;
import com.msa.account.domain.factory.dto.QFactoryDto_FactorySingleResponse;
import com.msa.common.global.util.CustomPage;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static com.msa.account.domain.factory.entity.QFactory.*;
import static com.msa.account.global.domain.entity.QAddress.*;
import static com.msa.account.global.domain.entity.QCommonOption.*;
import static com.msa.account.global.domain.entity.QGoldHarry.*;

public class FactoryRepositoryImpl implements CustomFactoryRepository{

    private final JPAQueryFactory query;

    public FactoryRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public Optional<FactoryDto.FactorySingleResponse> findByFactoryId(Long factoryId) {

        return Optional.ofNullable(query
                .select(new QFactoryDto_FactorySingleResponse(
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
    public CustomPage<FactoryDto.FactoryResponse> findAllFactory(Pageable pageable) {

        List<FactoryDto.FactoryResponse> content = query
                .select(new QFactoryDto_FactoryResponse(
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
                .where(factory.factoryDeleted.isFalse())
                .orderBy(factory.factoryName.desc())
                .fetch();

        JPAQuery<Long> countQuery = query
                .select(factory.count())
                .from(factory);

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }
}
