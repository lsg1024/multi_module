package com.msa.jewelry.product.internal.product.repository.work_grade_policy_group;

import com.msa.jewelry.product.internal.product.dto.ProductWorkGradePolicyGroupDto;

import java.util.List;

public interface CustomProductWorkGradePolicyGroup {
    List<ProductWorkGradePolicyGroupDto.Response> findByWorkGradePolicyGroupByProductIdOrderById(Long productId);
}
