package com.msa.product.local.color.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.product.global.kafka.KafkaProducer;
import com.msa.product.local.color.dto.ColorDto;
import com.msa.product.local.color.entity.Color;
import com.msa.product.local.color.repository.ColorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.msa.product.global.exception.ExceptionMessage.*;

@Service
@Transactional
public class ColorService {
    private final JwtUtil jwtUtil;
    private final KafkaProducer kafkaProducer;
    private final ColorRepository colorRepository;

    public ColorService(JwtUtil jwtUtil, KafkaProducer kafkaProducer, ColorRepository colorRepository) {
        this.jwtUtil = jwtUtil;
        this.kafkaProducer = kafkaProducer;
        this.colorRepository = colorRepository;
    }

    // 생성
    public void saveColor(ColorDto colorDto) {
        boolean existsByColorName = colorRepository.existsByColorName(colorDto.getName());
        if (existsByColorName) {
            throw new IllegalArgumentException(IS_EXIST);
        }

        Color color = Color.builder()
                .colorName(colorDto.getName())
                .colorNote(colorDto.getNote())
                .build();

        colorRepository.save(color);
    }

    // 단건 조회
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

    // 복수 조회
    @Transactional(readOnly = true)
    public List<ColorDto.ResponseSingle> getColors(String colorName) {
        return colorRepository.findAllOrderByAsc(colorName);
    }

    // 수정
    public void updateColor(Long colorId, ColorDto colorDto) {
        Color color = colorRepository.findById(colorId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        boolean existsByColorName = colorRepository.existsByColorName(colorDto.getName());
        if (!color.getColorName().equals(colorDto.getName()) && existsByColorName) {
            throw new IllegalArgumentException(IS_EXIST);
        }

        color.updateColor(colorDto);
    }

    // 삭제
    public void deleteColor(String accessToken, Long colorId) {
        String role = jwtUtil.getRole(accessToken);
        String tenantId = jwtUtil.getTenantId(accessToken);
        Color color = colorRepository.findById(colorId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        boolean deletable = color.isDeletable();
        if (deletable) {
            throw new IllegalArgumentException(CANNOT_DELETE_DEFAULT);
        }

        if (!role.equals("ADMIN")) {
            throw new IllegalArgumentException(NOT_ACCESS);
        }

        kafkaProducer.sendColorUpdate(tenantId, colorId);
    }

    public String getColorName(Long id) {
        return colorRepository.findByColorName(id);
    }
}
