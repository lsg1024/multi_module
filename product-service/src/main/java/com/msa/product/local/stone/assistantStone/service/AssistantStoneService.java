package com.msa.product.local.stone.assistantStone.service;

import com.msa.product.local.stone.assistantStone.dto.AssistantStoneDto;
import com.msa.product.local.stone.assistantStone.entity.AssistantStone;
import com.msa.product.local.stone.assistantStone.repository.AssistantStoneRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.msa.product.global.exception.ExceptionMessage.NOT_FOUND;

@Service
@Transactional
public class AssistantStoneService {

    private final AssistantStoneRepository assistantStoneRepository;

    public AssistantStoneService(AssistantStoneRepository assistantStoneRepository) {
        this.assistantStoneRepository = assistantStoneRepository;
    }

    public List<AssistantStoneDto.Response> getAssistantStoneAll() {
        List<AssistantStone> assistantStones = assistantStoneRepository.findAll(Sort.by(Sort.Direction.ASC, "assistanceStoneName"));
        List<AssistantStoneDto.Response> assistantDtos = new ArrayList<>();
        for (AssistantStone assistantStone : assistantStones) {
            AssistantStoneDto.Response assistantDto = new AssistantStoneDto.Response(
                    assistantStone.getAssistanceStoneId(),
                    assistantStone.getAssistanceStoneName(),
                    assistantStone.getAssistanceStoneNote());

            assistantDtos.add(assistantDto);
        }

        return assistantDtos;
    }

    public AssistantStoneDto.Response getAssistantStone(Long assistantId) {
        AssistantStone assistantStone = assistantStoneRepository.findById(assistantId)
                .orElseThrow(() -> new IllegalArgumentException("assistant: " + NOT_FOUND));
        return new AssistantStoneDto.Response(assistantStone.getAssistanceStoneId(), assistantStone.getAssistanceStoneName(), assistantStone.getAssistanceStoneNote());
    }
}
