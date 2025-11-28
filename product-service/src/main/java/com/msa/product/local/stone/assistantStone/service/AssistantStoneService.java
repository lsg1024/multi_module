package com.msa.product.local.stone.assistantStone.service;

import com.msa.common.global.util.AuthorityUserRoleUtil;
import com.msa.product.local.stone.assistantStone.dto.AssistantStoneDto;
import com.msa.product.local.stone.assistantStone.entity.AssistantStone;
import com.msa.product.local.stone.assistantStone.repository.AssistantStoneRepository;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.msa.product.global.exception.ExceptionMessage.NOT_ACCESS;
import static com.msa.product.global.exception.ExceptionMessage.NOT_FOUND;

@Service
@Transactional
public class AssistantStoneService {
    private final AuthorityUserRoleUtil authorityUserRoleUtil;
    private final AssistantStoneRepository assistantStoneRepository;

    public AssistantStoneService(AuthorityUserRoleUtil authorityUserRoleUtil, AssistantStoneRepository assistantStoneRepository) {
        this.authorityUserRoleUtil = authorityUserRoleUtil;
        this.assistantStoneRepository = assistantStoneRepository;
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public AssistantStoneDto.Response getAssistantStone(Long assistantId) {
        AssistantStone assistantStone = assistantStoneRepository.findById(assistantId)
                .orElseThrow(() -> new IllegalArgumentException("assistant: " + NOT_FOUND));
        return new AssistantStoneDto.Response(assistantStone.getAssistanceStoneId(), assistantStone.getAssistanceStoneName(), assistantStone.getAssistanceStoneNote());
    }

    public void createAssistantStone(String accessToken, AssistantStoneDto.Request assistantDto) {

        if (authorityUserRoleUtil.verification(accessToken)) {
            AssistantStone assistantStone = AssistantStone.builder()
                    .assistanceStoneName(assistantDto.getAssistantStoneName())
                    .assistanceStoneNote(assistantDto.getAssistantStoneNote())
                    .build();

            assistantStoneRepository.save(assistantStone);
        }
        throw new ForbiddenException(NOT_ACCESS);
    }

    public void updateAssistantStone(String accessToken, String assistantId, AssistantStoneDto.Request assistantDto) {
        if (authorityUserRoleUtil.verification(accessToken)) {

            Long newAssistantId = Long.parseLong(assistantId);

            AssistantStone assistantStone = assistantStoneRepository.findById(newAssistantId)
                    .orElseThrow(() -> new NotFoundException(NOT_FOUND));

            assistantStone.updateAssistantStone(assistantDto.getAssistantStoneName(), assistantStone.getAssistanceStoneNote());
        }
        throw new ForbiddenException(NOT_ACCESS);
    }

    public void deletedAssistantStone(String accessToken, String assistantId) {
        if (authorityUserRoleUtil.isAdmin(accessToken)) {
            Long newAssistantId = Long.parseLong(assistantId);
            AssistantStone assistantStone = assistantStoneRepository.findById(newAssistantId)
                    .orElseThrow(() -> new NotFoundException(NOT_FOUND));

            assistantStoneRepository.delete(assistantStone);
        }
        throw new IllegalArgumentException(NOT_ACCESS);
    }
}
