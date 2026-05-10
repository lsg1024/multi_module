package com.msa.jewelry.product.internal.goldPrice.service;

import com.msa.jewelry.product.internal.goldPrice.dto.GoldDto;
import com.msa.jewelry.product.internal.goldPrice.entity.Gold;
import com.msa.jewelry.product.internal.goldPrice.repository.GoldRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.msa.jewelry.product.internal.global.exception.ExceptionMessage.NOT_FOUND;

@Service
@Transactional
public class GoldService {

    private final GoldRepository goldRepository;

    public GoldService(GoldRepository goldRepository) {
        this.goldRepository = goldRepository;
    }

    @Transactional(readOnly = true)
    public Integer getGoldPrice() {
        Gold gold = goldRepository.findTopByOrderByGoldIdDesc()
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        return gold.getGoldPrice();
    }

    @Transactional(readOnly = true)
    public List<GoldDto> getGoldPrices() {
        List<Gold> allByOrderByCreateDateDesc = goldRepository.findAllByOrderByCreateDateDesc();

        List<GoldDto> goldDtos = new ArrayList<>();
        for (Gold gold : allByOrderByCreateDateDesc) {
            GoldDto goldDto = new GoldDto(gold.getGoldPrice(), gold.getCreateDate().toString());
            goldDtos.add(goldDto);
        }

        return goldDtos;
    }

    public void createGoldPrice(Integer newGoldPrice) {
        Gold gold = Gold.builder()
                .goldPrice(newGoldPrice)
                .build();

        goldRepository.save(gold);
    }

}
