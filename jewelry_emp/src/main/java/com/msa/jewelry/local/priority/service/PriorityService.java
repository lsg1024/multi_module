package com.msa.jewelry.local.priority.service;

import com.msa.common.global.util.AuthorityUserRoleUtil;
import com.msa.jewelry.local.priority.dto.PriorityDto;
import com.msa.jewelry.local.priority.entity.Priority;
import com.msa.jewelry.local.priority.repository.PriorityRepository;
import com.msa.jewelry.global.exception.NotAuthorityException;
import com.msa.jewelry.global.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.msa.jewelry.global.exception.ExceptionMessage.NOT_ACCESS;
import static com.msa.jewelry.global.exception.ExceptionMessage.NOT_FOUND;

@Service
@Transactional
public class PriorityService {

    private final AuthorityUserRoleUtil authorityUserRoleUtil;
    private final PriorityRepository priorityRepository;

    public PriorityService(AuthorityUserRoleUtil authorityUserRoleUtil, PriorityRepository priorityRepository) {
        this.authorityUserRoleUtil = authorityUserRoleUtil;
        this.priorityRepository = priorityRepository;
    }

    @Transactional(readOnly = true)
    public List<PriorityDto> findAllPriority() {
        List<Priority> priorities = priorityRepository.findAll();

        List<PriorityDto> priorityDtos = new ArrayList<>();
        for (Priority priority : priorities) {
            PriorityDto priorityDto = PriorityDto.builder()
                    .priorityId(String.valueOf(priority.getPriorityId()))
                    .priorityName(priority.getPriorityName())
                    .priorityDate(priority.getPriorityDate())
                    .build();

            priorityDtos.add(priorityDto);
        }
        return priorityDtos;
    }

    public void createPriority(String accessToken, PriorityDto.Request priorityDto) {
        if (!authorityUserRoleUtil.verification(accessToken)) {
            throw new NotAuthorityException(NOT_ACCESS);
        }

        Priority priority = Priority.builder()
                .priorityName(priorityDto.getPriorityName())
                .priorityDate(priorityDto.getPriorityDate())
                .build();

        priorityRepository.save(priority);
    }

    public void updatePriority(String accessToken, String priorityId, PriorityDto.Update priorityDto) {
        if (!authorityUserRoleUtil.verification(accessToken)) {
            throw new NotAuthorityException(NOT_ACCESS);
        }

        Long newPriorityId = Long.parseLong(priorityId);

        Priority priority = priorityRepository.findById(newPriorityId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));

        priority.updatePriority(priorityDto.getPriorityName(), priorityDto.getPriorityDate());
    }

    public void delete(String accessToken, String priorityId) {
        if (!authorityUserRoleUtil.verification(accessToken)) {
            throw new NotAuthorityException(NOT_ACCESS);
        }

        Long newPriorityId = Long.parseLong(priorityId);

        Priority priority = priorityRepository.findById(newPriorityId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));

        priorityRepository.delete(priority);
    }
}
