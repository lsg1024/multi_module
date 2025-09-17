package com.msa.order.local.priority.service;

import com.msa.order.local.priority.dto.PriorityDto;
import com.msa.order.local.priority.entitiy.Priority;
import com.msa.order.local.priority.repository.PriorityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PriorityService {

    private final PriorityRepository priorityRepository;

    public PriorityService(PriorityRepository priorityRepository) {
        this.priorityRepository = priorityRepository;
    }

    public List<PriorityDto> findAllPriority() {
        List<Priority> priorities = priorityRepository.findAll();

        List<PriorityDto> priorityDtos = new ArrayList<>();
        for (Priority priority : priorities) {
            PriorityDto priorityDto = PriorityDto.builder()
                    .priorityName(priority.getPriorityName())
                    .priorityDate(priority.getPriorityDate())
                    .build();

            priorityDtos.add(priorityDto);
        }
        return priorityDtos;
    }
}
