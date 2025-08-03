package com.msa.product.local.color.repository;

import com.msa.product.local.color.dto.ColorDto;

import java.util.List;

public interface CustomColorRepository {
    List<ColorDto.ResponseSingle> findAllOrderByAsc(String colorName);
}
