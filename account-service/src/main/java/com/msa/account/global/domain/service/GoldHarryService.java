package com.msa.account.global.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.msa.account.global.domain.dto.GoldHarryDto;
import com.msa.account.global.domain.entity.GoldHarry;
import com.msa.account.global.domain.repository.GoldHarryRepository;
import com.msa.account.global.exception.NotFoundException;
import com.msa.account.global.kafka.KafkaProducer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoldHarryService {

    private final GoldHarryRepository goldHarryRepository;
    private final KafkaProducer kafkaProducer;

    public void updateLoss(Long goldHarryId, GoldHarryDto.Update request) {
        GoldHarry goldHarry = goldHarryRepository.findById(goldHarryId)
                .orElseThrow(() -> new NotFoundException("해리 값을 찾을 수 없습니다."));

        if (!String.valueOf(goldHarry.getGoldHarryLoss()).equals(request.getGoldHarryLoss())) {
            goldHarry.updateLoss(request.getGoldHarryLoss());
            kafkaProducer.sendGoldHarryLossUpdated(goldHarryId, request.getGoldHarryLoss());
        }
    }

}
