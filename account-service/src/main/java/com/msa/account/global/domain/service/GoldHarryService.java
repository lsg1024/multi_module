package com.msa.account.global.domain.service;

import com.msa.account.global.domain.dto.GoldHarryDto;
import com.msa.account.global.domain.entity.GoldHarry;
import com.msa.account.global.domain.repository.GoldHarryRepository;
import com.msa.account.global.exception.NotFoundException;
import com.msa.account.global.kafka.KafkaProducer;
import com.msa.account.global.kafka.dto.KafkaEventDto;
import com.msacommon.global.jwt.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.msa.account.global.exception.ExceptionMessage.*;

@Service
@Transactional
public class GoldHarryService {

    private final JwtUtil jwtUtil;
    private final KafkaProducer kafkaProducer;
    private final GoldHarryRepository goldHarryRepository;

    public GoldHarryService(JwtUtil jwtUtil, KafkaProducer kafkaProducer, GoldHarryRepository goldHarryRepository) {
        this.jwtUtil = jwtUtil;
        this.kafkaProducer = kafkaProducer;
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
        String role = jwtUtil.getRole(accessToken);

        GoldHarry goldHarry;
        if (role.equals("ADMIN") || role.equals("USER")) {
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

//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    public void handleGoldHarryLossUpdateEvent(KafkaEventDto.UpdateLossDto kafkaEventDto) {
//        kafkaProducer.sendGoldHarryLossUpdated(kafkaEventDto.getTenantId(), kafkaEventDto.getGoldHarryId(), kafkaEventDto.getGoldHarryDto().getGoldHarryLoss());
//    }

}
