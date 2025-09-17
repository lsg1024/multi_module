package com.msa.order.local.order.repository;

import com.msa.order.local.sale.entity.dto.QSaleDto_StoneCountDto;
import com.msa.order.local.sale.entity.dto.SaleDto;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

import java.util.Collections;
import java.util.List;

import static com.msa.order.local.order.entity.QOrderStone.orderStone;

public class OrderStoneRepositoryImpl implements CustomOrderStoneRepository {

    private final JPAQueryFactory query;

    public OrderStoneRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public List<SaleDto.StoneCountDto> findStoneCountsByStockIds(List<Long> flowCodes) {

        if (flowCodes == null || flowCodes.isEmpty()) {
            return Collections.emptyList();
        }

        return query
                .select(new QSaleDto_StoneCountDto(
                        orderStone.stock.flowCode,
                        orderStone.mainStone,
                        orderStone.stoneQuantity.sum()
                ))
                .from(orderStone)
                .where(orderStone.stock.flowCode.in(flowCodes))
                .groupBy(orderStone.stock.flowCode, orderStone.mainStone)
                .fetch();
    }
}
