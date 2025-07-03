package com.account.domain.store.repository;

import com.account.global.domain.dto.AccountDto;
import com.account.global.domain.dto.QAccountDto_accountInfo;
import com.msacommon.global.util.CustomPage;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static com.account.domain.store.entity.QStore.store;
import static com.account.global.domain.entity.QAddress.address;
import static com.account.global.domain.entity.QCommonOption.commonOption;
import static com.account.global.domain.entity.QGoldLoss.goldLoss;

public class StoreRepositoryImpl implements CustomStoreRepository {

    private final JPAQueryFactory query;

    public StoreRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public Optional<AccountDto.accountInfo> findByStoreId(Long storeId) {
        return Optional.ofNullable(getStoreSelect()
                .from(store)
                .join(store.address, address)
                .join(store.commonOption, commonOption)
                .join(store.commonOption.goldLoss, goldLoss)
                .where(store.storeId.eq(storeId).and(store.storeDeleted.isFalse()))
                .fetchOne());

    }

    @Override
    public CustomPage<AccountDto.accountInfo> findAllStore(Pageable pageable) {

        List<AccountDto.accountInfo> content = getStoreSelect()
                .from(store)
                .join(store.address, address)
                .join(store.commonOption, commonOption)
                .join(store.commonOption.goldLoss, goldLoss)
                .where(store.storeDeleted.isFalse())
                .orderBy(store.storeName.desc())
                .fetch();

        JPAQuery<Long> countQuery = query
                .select(store.count())
                .from(store);

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }
    private JPAQuery<AccountDto.accountInfo> getStoreSelect() {
        return query
                .select(new QAccountDto_accountInfo(
                        store.createDate,
                        store.createdBy,
                        store.storeName,
                        store.storeOwnerName,
                        store.storeContactNumber1,
                        store.storeContactNumber2,
                        store.storeFaxNumber,
                        Expressions.stringTemplate(
                                "concat({0}, ' ', {1}, ' ', {2})",
                                store.address.addressZipCode,
                                store.address.addressBasic,
                                store.address.addressAdd
                        ),
                        store.commonOption.optionTradeNote,
                        store.storeNote,
                        store.commonOption.optionLevel.stringValue(),
                        store.commonOption.optionTradeType.stringValue(),
                        store.commonOption.goldLoss.loss.stringValue()));
    }
}
