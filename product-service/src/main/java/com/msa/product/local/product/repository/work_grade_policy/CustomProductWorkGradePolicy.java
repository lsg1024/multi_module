package com.msa.product.local.product.repository.work_grade_policy;

import com.msa.product.local.product.dto.ProductWorkGradePolicyDto;

import java.util.List;

public interface CustomProductWorkGradePolicy {
    List<ProductWorkGradePolicyDto.Response> findWorkGradePolicyByProductId(Long productId);
}
