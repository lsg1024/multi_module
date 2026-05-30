package com.msa.jewelry.local.color.repository;

import com.msa.jewelry.local.color.dto.ColorDto;

import java.util.List;

public interface CustomColorRepository {
    List<ColorDto.ResponseSingle> findAllOrderByAsc(String colorName);
}
