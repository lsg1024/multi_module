package com.msa.account.domain.store.repository;

import com.msa.account.global.domain.dto.AccountDto;
import com.msa.account.global.domain.dto.QAccountDto_accountInfo;
import com.msa.account.domain.store.entity.QStore;
import com.msa.account.global.domain.entity.QAddress;
import com.msa.account.global.domain.entity.QCommonOption;
import com.msa.account.global.domain.entity.QGoldLoss;
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
    public Optional<AccountDto.accountInfo> findByStoreId(Long storeId) {
        return Optional.ofNullable(getStoreSelect()
                .from(QStore.store)
                .join(QStore.store.address, QAddress.address)
                .join(QStore.store.commonOption, QCommonOption.commonOption)
                .join(QStore.store.commonOption.goldLoss, QGoldLoss.goldLoss)
                .where(QStore.store.storeId.eq(storeId).and(QStore.store.storeDeleted.isFalse()))
                .fetchOne());

    }

    @Override
    public CustomPage<AccountDto.accountInfo> findAllStore(Pageable pageable) {

        List<AccountDto.accountInfo> content = getStoreSelect()
                .from(QStore.store)
                .join(QStore.store.address, QAddress.address)
                .join(QStore.store.commonOption, QCommonOption.commonOption)
                .join(QStore.store.commonOption.goldLoss, QGoldLoss.goldLoss)
                .where(QStore.store.storeDeleted.isFalse())
                .orderBy(QStore.store.storeName.desc())
                .fetch();

        JPAQuery<Long> countQuery = query
                .select(QStore.store.count())
                .from(QStore.store);

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }
    private JPAQuery<AccountDto.accountInfo> getStoreSelect() {
        return query
                .select(new QAccountDto_accountInfo(
                        QStore.store.createDate,
                        QStore.store.createdBy,
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
                        QStore.store.commonOption.optionTradeNote,
                        QStore.store.storeNote,
                        QStore.store.commonOption.optionLevel.stringValue(),
                        QStore.store.commonOption.optionTradeType.stringValue(),
                        QStore.store.commonOption.goldLoss.loss.stringValue()));
    }
}
