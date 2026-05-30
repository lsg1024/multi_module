package com.msa.jewelry.local.assistant_stone.service;

import com.msa.jewelry.local.assistant_stone.dto.AssistantStoneDto;
import com.msa.jewelry.local.assistant_stone.dto.AssistantStoneView;

import java.util.List;

public interface AssistantStoneService {

    List<AssistantStoneDto.Response> getAssistantStoneAll();

    AssistantStoneDto.Response getAssistantStone(Long assistantId);

    void createAssistantStone(String accessToken, AssistantStoneDto.Request assistantDto);

    void updateAssistantStone(String accessToken, String assistantId, AssistantStoneDto.Request assistantDto);

    void deletedAssistantStone(String accessToken, String assistantId);

    AssistantStoneView getAssistantStoneView(Long assistantStoneId);
}
