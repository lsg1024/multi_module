package com.msa.jewelry.product.internal.color.repository;

import com.msa.jewelry.product.internal.color.dto.ColorDto;

import java.util.List;

public interface CustomColorRepository {
    List<ColorDto.ResponseSingle> findAllOrderByAsc(String colorName);
}
