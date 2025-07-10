package com.msa.account.domain.store.repository;

import com.msa.account.domain.store.dto.QStoreDto_StoreResponse;
import com.msa.account.domain.store.dto.QStoreDto_StoreSingleResponse;
import com.msa.account.domain.store.dto.StoreDto;
import com.msa.account.domain.store.entity.QAdditionalOption;
import com.msa.account.global.domain.dto.AccountDto;
import com.msa.account.global.domain.dto.QAccountDto_accountInfo;
import com.msa.account.domain.store.entity.QStore;
import com.msa.account.global.domain.entity.QAddress;
import com.msa.account.global.domain.entity.QCommonOption;
import com.msa.account.global.domain.entity.QGoldHarry;
import com.msacommon.global.util.CustomPage;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public class StoreRepositoryImpl implements CustomStoreRepository {

    private final JPAQueryFactory query;

    public StoreRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public Optional<StoreDto.StoreSingleResponse> findByStoreId(Long storeId) {
        return Optional.ofNullable(query
                .select(new QStoreDto_StoreSingleResponse(
                        QStore.store.storeId.stringValue(),
                        QStore.store.storeName,
                        QStore.store.storeOwnerName,
                        QStore.store.storePhoneNumber,
                        QStore.store.storeContactNumber1,
                        QStore.store.storeContactNumber2,
                        QStore.store.storeFaxNumber,
                        QStore.store.storeNote,

                        QStore.store.address.addressId.stringValue(),
                        QStore.store.address.addressZipCode,
                        QStore.store.address.addressBasic,
                        QStore.store.address.addressAdd,

                        QStore.store.commonOption.commonOptionId.stringValue(),
                        QStore.store.commonOption.optionTradeType.stringValue(),
                        QStore.store.commonOption.optionLevel.stringValue(),
                        QStore.store.commonOption.goldHarry.goldHarryId.stringValue(),
                        QStore.store.commonOption.goldHarry.goldHarryLoss.stringValue(),

                        QStore.store.additionalOption.optionId.stringValue(),
                        QStore.store.additionalOption.optionApplyPastSales,
                        QStore.store.additionalOption.optionMaterialId.stringValue(),
                        QStore.store.additionalOption.optionMaterialName))
                .from(QStore.store)
                .join(QStore.store.address, QAddress.address)
                .join(QStore.store.commonOption, QCommonOption.commonOption)
                .join(QStore.store.additionalOption, QAdditionalOption.additionalOption)
                .join(QStore.store.commonOption.goldHarry, QGoldHarry.goldHarry)
                .where(QStore.store.storeDeleted.isFalse())
                .fetchOne());
    }

    @Override
    public CustomPage<StoreDto.StoreResponse> findAllStore(Pageable pageable) {

        List<StoreDto.StoreResponse> content = query
                .select(new QStoreDto_StoreResponse(
                        QStore.store.storeName,
                        QStore.store.storeOwnerName,
                        QStore.store.storeContactNumber1,
                        QStore.store.storeContactNumber2,
                        QStore.store.storeFaxNumber,
                        Expressions.stringTemplate(
                                "concat({0}, ' ', {1}, ' ', {2})",
                                QStore.store.address.addressZipCode,
                                QStore.store.address.addressBasic,
                                QStore.store.address.addressAdd
                        ),
                        QStore.store.storeNote,
                        QStore.store.commonOption.optionTradeType.stringValue(),
                        QStore.store.commonOption.optionLevel.stringValue(),
                        QStore.store.commonOption.goldHarryLoss))
                .from(QStore.store)
                .join(QStore.store.address, QAddress.address)
                .join(QStore.store.commonOption, QCommonOption.commonOption)
                .where(QStore.store.storeDeleted.isFalse())
                .orderBy(QStore.store.storeName.desc())
                .fetch();

        content.forEach(StoreDto.StoreResponse::getTradeTypeTitle);
        content.forEach(StoreDto.StoreResponse::getLevelTypeLevel);

        JPAQuery<Long> countQuery = query
                .select(QStore.store.count())
                .from(QStore.store);

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }
}
