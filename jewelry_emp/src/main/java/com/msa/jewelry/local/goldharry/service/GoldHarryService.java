package com.msa.jewelry.local.goldharry.service;

import com.msa.jewelry.local.goldharry.dto.GoldHarryDto;
import com.msa.jewelry.local.goldharry.entity.GoldHarry;
import com.msa.jewelry.local.goldharry.repository.GoldHarryRepository;
import com.msa.jewelry.global.exception.NotFoundException;
import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.util.AuthorityUserRoleUtil;
import com.msa.jewelry.global.exception.NotAuthorityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.msa.jewelry.global.exception.ExceptionMessage.BATCH_FAIL;
import static com.msa.jewelry.global.exception.ExceptionMessage.DEFAULT_HARRY;
import static com.msa.jewelry.global.exception.ExceptionMessage.WRONG_HARRY;

@Slf4j
@Service
@Transactional
public class GoldHarryService {

    private final JwtUtil jwtUtil;
    private final JobLauncher jobLauncher;
    private final Job updateStoreGoldHarryLossJob;
    private final Job deleteGoldHarryJob;
    private final AuthorityUserRoleUtil authorityUserRoleUtil;
    private final GoldHarryRepository goldHarryRepository;

    public GoldHarryService(JwtUtil jwtUtil,
                            JobLauncher jobLauncher,
                            @Qualifier("updateStoreGoldHarryLossJob") Job updateStoreGoldHarryLossJob,
                            @Qualifier("deleteGoldHarryJob") Job deleteGoldHarryJob,
                            AuthorityUserRoleUtil authorityUserRoleUtil,
                            GoldHarryRepository goldHarryRepository) {
        this.jwtUtil = jwtUtil;
        this.jobLauncher = jobLauncher;
        this.updateStoreGoldHarryLossJob = updateStoreGoldHarryLossJob;
        this.deleteGoldHarryJob = deleteGoldHarryJob;
        this.authorityUserRoleUtil = authorityUserRoleUtil;
        this.goldHarryRepository = goldHarryRepository;
    }

    public void updateLoss(String accessToken, Long goldHarryId, GoldHarryDto.Update request) {
        String tenantId = jwtUtil.getTenantId(accessToken);

        GoldHarry goldHarry = goldHarryRepository.findById(goldHarryId)
                .orElseThrow(() -> new NotFoundException(WRONG_HARRY));

        if (!String.valueOf(goldHarry.getGoldHarryLoss()).equals(request.getGoldHarry())) {
            goldHarry.updateLoss(request.getGoldHarry());
            goldHarryRepository.save(goldHarry);

            try {
                JobParameters params = new JobParametersBuilder()
                        .addString("tenantId", tenantId)
                        .addLong("goldHarryId", goldHarryId)
                        .addString("newGoldHarry", request.getGoldHarry())
                        .addLong("timestamp", System.currentTimeMillis())
                        .toJobParameters();
                jobLauncher.run(updateStoreGoldHarryLossJob, params);
            } catch (Exception e) {
                log.error("updateStoreGoldHarryLossJob 실행 실패: goldHarryId={}", goldHarryId, e);
                throw new IllegalStateException(BATCH_FAIL, e);
            }
        }
    }

    public void delete(String accessToken, String goldHarryId) {
        String tenantId = jwtUtil.getTenantId(accessToken);

        if (authorityUserRoleUtil.verification(accessToken)) {
            GoldHarry goldHarry = goldHarryRepository.findById(Long.valueOf(goldHarryId))
                    .orElseThrow(() -> new NotFoundException(WRONG_HARRY));

            if (goldHarry.getDefaultOption()) {
                throw new IllegalArgumentException(DEFAULT_HARRY);
            }
            goldHarryRepository.delete(goldHarry);

            try {
                JobParameters params = new JobParametersBuilder()
                        .addString("tenantId", tenantId)
                        .addString("goldHarryId", goldHarryId)
                        .addLong("timestamp", System.currentTimeMillis())
                        .toJobParameters();
                jobLauncher.run(deleteGoldHarryJob, params);
            } catch (Exception e) {
                log.error("deleteGoldHarryJob 실행 실패: goldHarryId={}", goldHarryId, e);
                throw new IllegalStateException(BATCH_FAIL, e);
            }
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
                    .goldHarryLoss(goldHarryDto.getGoldHarry())
                    .build();
            goldHarryRepository.save(goldHarry);
            return;
        }
        throw new NotAuthorityException("생성 권한이 없는 사용자 입니다.");
    }
}
