package com.msa.order.local.sale.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.order.local.sale.entity.dto.SaleDto;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomSaleRepository {
    CustomPage<SaleDto.Response> findSales(SaleDto.Condition condition, Pageable pageable);

    List<SaleDto.SaleDetailDto> findSalePast(Long storeId, Long productId, String materialName);
}
