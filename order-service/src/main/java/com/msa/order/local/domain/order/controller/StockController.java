package com.msa.order.local.domain.order.controller;

import com.msa.order.local.domain.order.service.StockService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }



}
