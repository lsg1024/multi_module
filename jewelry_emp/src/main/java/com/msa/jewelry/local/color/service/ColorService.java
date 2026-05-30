package com.msa.jewelry.local.color.service;

import com.msa.jewelry.local.color.dto.ColorDto;

import java.util.List;

public interface ColorService {

    void saveColor(ColorDto colorDto);

    ColorDto.ResponseSingle getColor(Long colorId);

    List<ColorDto.ResponseSingle> getColors(String colorName);

    void updateColor(Long colorId, ColorDto colorDto);

    void deleteColor(String accessToken, Long colorId);

    String getColorName(Long colorId);

    Long getColorIdByName(String name);
}
