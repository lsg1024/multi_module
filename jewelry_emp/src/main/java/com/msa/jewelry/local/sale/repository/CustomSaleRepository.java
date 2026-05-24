package com.msa.jewelry.local.sale.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.global.excel.dto.SaleExcelDto;
import com.msa.jewelry.local.sale.dto.SaleDto;
import com.msa.jewelry.local.sale.dto.SaleItemResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomSaleRepository {
    CustomPage<SaleItemResponse.SaleItem> findSales(SaleDto.Condition condition, Pageable pageable);
    List<SaleItemResponse.SaleItem> findAllSales(SaleDto.Condition condition);
    List<SaleDto.SaleStoreInfo> findSaleStores(String startAt, String endAt);
    List<SaleItemResponse> findPrintSales(String saleCode);
    List<SaleDto.SaleDetailDto> findSalePast(Long storeId, Long productId, String materialName);
    List<SaleExcelDto> findSalesForExcel(SaleDto.Condition condition);
}
