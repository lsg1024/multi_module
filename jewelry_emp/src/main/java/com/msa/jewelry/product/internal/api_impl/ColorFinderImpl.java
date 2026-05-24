package com.msa.jewelry.product.internal.api_impl;

import com.msa.jewelry.product.api.ColorFinder;
import com.msa.jewelry.product.internal.color.entity.Color;
import com.msa.jewelry.product.internal.color.repository.ColorRepository;
import com.msa.jewelry.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ColorFinderImpl implements ColorFinder {

    private final ColorRepository colorRepository;

    @Override
    public String getColorName(Long colorId) {
        return colorRepository.findById(colorId)
                .map(Color::getColorName)
                .orElseThrow(() -> new NotFoundException("색상 미존재: colorId=" + colorId));
    }

    @Override
    public Long findColorIdByName(String colorName) {
        return colorRepository.findByColorNameIgnoreCase(colorName)
                .map(Color::getColorId)
                .orElse(null);
    }
}
