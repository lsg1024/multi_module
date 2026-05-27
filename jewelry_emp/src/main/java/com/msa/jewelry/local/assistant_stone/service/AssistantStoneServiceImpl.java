package com.msa.jewelry.local.assistant_stone.service;

import com.msa.common.global.util.AuthorityUserRoleUtil;
import com.msa.jewelry.local.assistant_stone.dto.AssistantStoneDto;
import com.msa.jewelry.local.assistant_stone.dto.AssistantStoneView;
import com.msa.jewelry.local.assistant_stone.entity.AssistantStone;
import com.msa.jewelry.local.assistant_stone.repository.AssistantStoneRepository;
import com.msa.jewelry.global.exception.NotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.msa.jewelry.global.exception.ExceptionMessage.NOT_ACCESS;
import static com.msa.jewelry.global.exception.ExceptionMessage.NOT_FOUND;

@Service
@Transactional
public class AssistantStoneServiceImpl implements AssistantStoneService {
    private final AuthorityUserRoleUtil authorityUserRoleUtil;
    private final AssistantStoneRepository assistantStoneRepository;

    public AssistantStoneServiceImpl(AuthorityUserRoleUtil authorityUserRoleUtil, AssistantStoneRepository assistantStoneRepository) {
        this.authorityUserRoleUtil = authorityUserRoleUtil;
        this.assistantStoneRepository = assistantStoneRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssistantStoneDto.Response> getAssistantStoneAll() {
        List<AssistantStone> assistantStones = assistantStoneRepository.findAll(Sort.by(Sort.Direction.ASC, "assistanceStoneName"));
        List<AssistantStoneDto.Response> assistantDtos = new ArrayList<>();
        for (AssistantStone assistantStone : assistantStones) {
            assistantDtos.add(new AssistantStoneDto.Response(
                    assistantStone.getAssistanceStoneId(),
                    assistantStone.getAssistanceStoneName(),
                    assistantStone.getAssistanceStoneNote()));
        }
        return assistantDtos;
    }

    @Override
    @Transactional(readOnly = true)
    public AssistantStoneDto.Response getAssistantStone(Long assistantId) {
        AssistantStone assistantStone = assistantStoneRepository.findById(assistantId)
                .orElseThrow(() -> new IllegalArgumentException("assistant: " + NOT_FOUND));
        return new AssistantStoneDto.Response(assistantStone.getAssistanceStoneId(), assistantStone.getAssistanceStoneName(), assistantStone.getAssistanceStoneNote());
    }

    @Override
    public void createAssistantStone(String accessToken, AssistantStoneDto.Request assistantDto) {
        if (!authorityUserRoleUtil.verification(accessToken)) {
            throw new IllegalArgumentException(NOT_ACCESS);
        }
        AssistantStone assistantStone = AssistantStone.builder()
                .assistanceStoneName(assistantDto.getAssistantStoneName())
                .assistanceStoneNote(assistantDto.getAssistantStoneNote())
                .build();
        assistantStoneRepository.save(assistantStone);
    }

    @Override
    public void updateAssistantStone(String accessToken, String assistantId, AssistantStoneDto.Request assistantDto) {
        if (!authorityUserRoleUtil.verification(accessToken)) {
            throw new IllegalArgumentException(NOT_ACCESS);
        }
        Long newAssistantId = Long.parseLong(assistantId);
        AssistantStone assistantStone = assistantStoneRepository.findById(newAssistantId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
        assistantStone.updateAssistantStone(assistantDto.getAssistantStoneName(), assistantStone.getAssistanceStoneNote());
    }

    @Override
    public void deletedAssistantStone(String accessToken, String assistantId) {
        if (!authorityUserRoleUtil.isAdmin(accessToken)) {
            throw new IllegalArgumentException(NOT_ACCESS);
        }
        Long newAssistantId = Long.parseLong(assistantId);
        AssistantStone assistantStone = assistantStoneRepository.findById(newAssistantId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
        assistantStoneRepository.delete(assistantStone);
    }

    @Override
    @Transactional(readOnly = true)
    public AssistantStoneView getAssistantStoneView(Long assistantStoneId) {
        AssistantStone entity = assistantStoneRepository.findById(assistantStoneId)
                .orElseThrow(() -> new com.msa.jewelry.global.exception.NotFoundException("보조석 미존재: assistantStoneId=" + assistantStoneId));
        return new AssistantStoneView(
                entity.getAssistanceStoneId(),
                entity.getAssistanceStoneName(),
                entity.getAssistanceStoneNote()
        );
    }
}
