package com.msa.jewelry.local.stock.service;

import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.local.stock.dto.InventoryDto;
import com.msa.jewelry.local.stock.dto.StockDto;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;

public interface StockService {

    Map<String, Integer> getStockCountByProductNames(List<String> productNames);

    List<StockDto.ResponseDetail> getDetailStock(List<Long> flowCodes);

    CustomPage<StockDto.Response> getStocks(String input, String searchField, String startAt, String endAt, String factoryName, String storeName, String setTypeName, String colorName, String classificationName, String materialName, String sortField, String sort, String orderStatus, Pageable pageable);

    CustomPage<StockDto.Response> getPastRentalHistory(String input, String searchField, String startAt, String endAt, String factoryName, String storeName, String setTypeName, String colorName, String classificationName, String materialName, String sortField, String sort,  Pageable pageable);

    void updateStock(String accessToken, Long flowCode, StockDto.updateStockRequest updateStock);

    void updateOrderToStock(String accessToken, Long flowCode, String orderType, StockDto.StockRegisterRequest stockDto);

    void saveStock(String accessToken, String orderType, StockDto.Request stockDto);

    void stockToRental(String accessToken, Long flowCode, StockDto.StockRentalRequest stockRentalDto);

    void stockToDelete(String accessToken, Long flowCode);

    void rentalToReturn(String accessToken, Long flowCode, String orderType);

    void rollBackStock(String accessToken, Long flowCode, String orderType);

    List<String> getFilterFactories(String startAt, String endAt, String orderStatus);

    List<String> getFilterStores(String startAt, String endAt, String orderStatus);

    List<String> getFilterSetType(String startAt, String endAt, String orderStatus);

    List<String> getFilterColors(String startAt, String endAt, String orderStatus);

    List<String> getFilterClassifications(String startAt, String endAt, String orderStatus);

    List<String> getFilterMaterials(String startAt, String endAt, String orderStatus);

    CustomPage<InventoryDto.Response> getInventoryStocks(
            String searchField, String searchValue,
            String sortField, String sortOrder,
            String stockChecked, String orderStatus,
            String materialName, Pageable pageable);

    List<String> getInventoryMaterials();

    InventoryDto.ResetResponse prepareInventoryCheck();

    InventoryDto.CheckResponse checkStock(Long flowCode);

    InventoryDto.StatisticsResponse getInventoryStatistics();

}
