package com.msa.order.local.priority.service;

import com.msa.common.global.util.AuthorityUserRoleUtil;
import com.msa.order.local.priority.dto.PriorityDto;
import com.msa.order.local.priority.entitiy.Priority;
import com.msa.order.local.priority.repository.PriorityRepository;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.msa.order.global.exception.ExceptionMessage.NOT_ACCESS;
import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;

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

        if (authorityUserRoleUtil.verification(accessToken)) {
            Priority priority = Priority.builder()
                    .priorityName(priorityDto.getPriorityName())
                    .priorityDate(priorityDto.getPriorityDate())
                    .build();

            priorityRepository.save(priority);
        }
        throw new ForbiddenException(NOT_ACCESS);
    }

    public void updatePriority(String accessToken, String priorityId, PriorityDto.Update priorityDto) {
        if (authorityUserRoleUtil.verification(accessToken)) {

            Long newPriorityId = Long.parseLong(priorityId);

            Priority priority = priorityRepository.findById(newPriorityId)
                    .orElseThrow(() -> new NotFoundException(NOT_FOUND));

            priority.updatePriority(priorityDto.getPriorityName(), priorityDto.getPriorityDate());
        }
        throw new ForbiddenException(NOT_ACCESS);
    }

    public void delete(String accessToken, String priorityId) {
        if (authorityUserRoleUtil.verification(accessToken)) {

            Long newPriorityId = Long.parseLong(priorityId);

            Priority priority = priorityRepository.findById(newPriorityId)
                    .orElseThrow(() -> new NotFoundException(NOT_FOUND));

            priorityRepository.delete(priority);
        }
        throw new ForbiddenException(NOT_ACCESS);
    }
}
