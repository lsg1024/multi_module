package com.msa.jewelry.local.color.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.jewelry.global.exception.NotFoundException;
import com.msa.jewelry.local.color.dto.ColorDto;
import com.msa.jewelry.local.color.entity.Color;
import com.msa.jewelry.local.color.repository.ColorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.msa.jewelry.global.exception.ExceptionMessage.*;

@Slf4j
@Service
@Transactional
public class ColorServiceImpl implements ColorService {
    private final JwtUtil jwtUtil;
    private final JobLauncher jobLauncher;
    private final Job updateColorJob;
    private final ColorRepository colorRepository;

    public ColorServiceImpl(JwtUtil jwtUtil,
                            JobLauncher jobLauncher,
                            @Qualifier("updateColorJob") Job updateColorJob,
                            ColorRepository colorRepository) {
        this.jwtUtil = jwtUtil;
        this.jobLauncher = jobLauncher;
        this.updateColorJob = updateColorJob;
        this.colorRepository = colorRepository;
    }

    @Override
    public void saveColor(ColorDto colorDto) {
        if (colorRepository.existsByColorName(colorDto.getName())) {
            throw new IllegalArgumentException(IS_EXIST);
        }
        Color color = Color.builder()
                .colorName(colorDto.getName())
                .colorNote(colorDto.getNote())
                .build();
        colorRepository.save(color);
    }

    @Override
    @Transactional(readOnly = true)
    public ColorDto.ResponseSingle getColor(Long colorId) {
        Color color = colorRepository.findById(colorId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
        return ColorDto.ResponseSingle.builder()
                .colorId(String.valueOf(colorId))
                .colorName(color.getColorName())
                .colorNote(color.getColorNote())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ColorDto.ResponseSingle> getColors(String colorName) {
        return colorRepository.findAllOrderByAsc(colorName);
    }

    @Override
    public void updateColor(Long colorId, ColorDto colorDto) {
        Color color = colorRepository.findById(colorId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
        if (!color.getColorName().equals(colorDto.getName())
                && colorRepository.existsByColorName(colorDto.getName())) {
            throw new IllegalArgumentException(IS_EXIST);
        }
        color.updateColor(colorDto);
    }

    @Override
    public void deleteColor(String accessToken, Long colorId) {
        String role = jwtUtil.getRole(accessToken);
        String tenantId = jwtUtil.getTenantId(accessToken);
        Color color = colorRepository.findById(colorId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
        if (color.isDeletable()) {
            throw new IllegalArgumentException(CANNOT_DELETE_DEFAULT);
        }
        if (!role.equals("ADMIN")) {
            throw new IllegalArgumentException(NOT_ACCESS);
        }

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("tenantId", tenantId)
                    .addLong("colorId", colorId)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(updateColorJob, jobParameters);
        } catch (Exception e) {
            log.error("updateColorJob 실행 실패: colorId={}", colorId, e);
            throw new IllegalStateException(BATCH_FAIL, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getColorName(Long colorId) {
        return colorRepository.findById(colorId)
                .map(Color::getColorName)
                .orElseThrow(() -> new NotFoundException("색상 미존재: colorId=" + colorId));
    }

    @Override
    @Transactional(readOnly = true)
    public Long getColorIdByName(String name) {
        return colorRepository.findByColorNameIgnoreCase(name)
                .map(Color::getColorId)
                .orElse(null);
    }
}
