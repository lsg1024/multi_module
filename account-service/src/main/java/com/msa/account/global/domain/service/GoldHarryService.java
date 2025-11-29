package com.msa.account.global.domain.service;

import com.msa.account.global.domain.dto.GoldHarryDto;
import com.msa.account.global.domain.entity.GoldHarry;
import com.msa.account.global.domain.repository.GoldHarryRepository;
import com.msa.account.global.exception.NotFoundException;
import com.msa.account.global.kafka.KafkaProducer;
import com.msa.account.global.kafka.dto.KafkaEventDto;
import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.util.AuthorityUserRoleUtil;
import jakarta.ws.rs.ForbiddenException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.msa.account.global.exception.ExceptionMessage.DEFAULT_HARRY;
import static com.msa.account.global.exception.ExceptionMessage.WRONG_HARRY;

@Service
@Transactional
public class GoldHarryService {

    private final JwtUtil jwtUtil;
    private final KafkaProducer kafkaProducer;
    private final AuthorityUserRoleUtil authorityUserRoleUtil;
    private final GoldHarryRepository goldHarryRepository;

    public GoldHarryService(JwtUtil jwtUtil, KafkaProducer kafkaProducer, AuthorityUserRoleUtil authorityUserRoleUtil, GoldHarryRepository goldHarryRepository) {
        this.jwtUtil = jwtUtil;
        this.kafkaProducer = kafkaProducer;
        this.authorityUserRoleUtil = authorityUserRoleUtil;
        this.goldHarryRepository = goldHarryRepository;
    }

    public void updateLoss(String accessToken, Long goldHarryId, GoldHarryDto.Update request) {

        String tenantId = jwtUtil.getTenantId(accessToken);

        GoldHarry goldHarry = goldHarryRepository.findById(goldHarryId)
                .orElseThrow(() -> new NotFoundException(WRONG_HARRY));

        if (!String.valueOf(goldHarry.getGoldHarryLoss()).equals(request.getGoldHarryLoss())) {
            goldHarry.updateLoss(request.getGoldHarryLoss());
            goldHarryRepository.save(goldHarry);

            KafkaEventDto.UpdateLossDto kafkaEventDto = KafkaEventDto.UpdateLossDto.builder()
                    .goldHarryId(goldHarryId)
                    .tenantId(tenantId)
                    .goldHarryDto(request)
                    .build();

            kafkaProducer.sendGoldHarryLossUpdated(kafkaEventDto.getTenantId(), kafkaEventDto.getGoldHarryId(), kafkaEventDto.getGoldHarryDto().getGoldHarryLoss());
        }
    }

    public void delete(String accessToken, String goldHarryId) {
        String tenantId = jwtUtil.getTenantId(accessToken);

        GoldHarry goldHarry;
        if (authorityUserRoleUtil.verification(accessToken)) {
            goldHarry = goldHarryRepository.findById(Long.valueOf(goldHarryId))
                    .orElseThrow(() -> new NotFoundException(WRONG_HARRY));

            if (goldHarry.getDefaultOption()) {
                throw new IllegalArgumentException(DEFAULT_HARRY);
            }
            goldHarryRepository.delete(goldHarry);

            //batch 작업
            kafkaProducer.sendGoldHarryDeleted(tenantId, goldHarryId);

        }
    }

    @Transactional(readOnly = true)
    public List<GoldHarryDto.Response> getGoldHarries() {
        List<GoldHarry> goldHarries = goldHarryRepository.findAll(Sort.by(Sort.Direction.ASC, "goldHarryLoss"));
        List<GoldHarryDto.Response> goldHarryList = new ArrayList<>();
        for (GoldHarry goldHarry : goldHarries) {
            GoldHarryDto.Response goldHarryDto = new GoldHarryDto.Response(goldHarry.getGoldHarryId().toString(), goldHarry.getGoldHarryLoss().toPlainString());
            goldHarryList.add(goldHarryDto);
        }
        return goldHarryList;
    }

    public void createHarry(String accessToken, GoldHarryDto.Request goldHarryDto) {
        if (authorityUserRoleUtil.verification(accessToken)) {
            GoldHarry goldHarry = GoldHarry.builder()
                    .goldHarryLoss(goldHarryDto.getGoldHarryLoss())
                    .build();
            goldHarryRepository.save(goldHarry);
            return;
        }
        throw new ForbiddenException("생성 권한이 없는 사용자 입니다.");
    }
}
