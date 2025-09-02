package com.msa.account.local.store.repository;

import com.msa.account.local.store.dto.QStoreDto_StoreResponse;
import com.msa.account.local.store.dto.QStoreDto_StoreSingleResponse;
import com.msa.account.local.store.dto.StoreDto;
import com.msa.common.global.util.CustomPage;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static com.msa.account.local.store.entity.QAdditionalOption.*;
import static com.msa.account.local.store.entity.QStore.*;
import static com.msa.account.global.domain.entity.QAddress.*;
import static com.msa.account.global.domain.entity.QCommonOption.*;
import static com.msa.account.global.domain.entity.QGoldHarry.*;

public class StoreRepositoryImpl implements CustomStoreRepository {

    private final JPAQueryFactory query;

    public StoreRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public Optional<StoreDto.StoreSingleResponse> findByStoreId(Long storeId) {
        return Optional.ofNullable(query
                .select(new QStoreDto_StoreSingleResponse(
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
                .join(store.address, address)
                .join(store.commonOption, commonOption)
                .join(store.additionalOption, additionalOption)
                .join(store.commonOption.goldHarry, goldHarry)
                .where(store.storeId.eq(storeId).and(store.storeDeleted.isFalse()))
                .fetchOne());
    }

    @Override
    public CustomPage<StoreDto.StoreResponse> findAllStore(String name, Pageable pageable) {

        BooleanExpression storeName = name != null ? store.storeName.contains(name) : null;

        List<StoreDto.StoreResponse> content = query
                .select(new QStoreDto_StoreResponse(
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
                .join(store.address, address)
                .join(store.commonOption, commonOption)
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
}
