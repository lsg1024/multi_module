package com.msa.jewelry.local.product.repository.work_grade_policy_group;

import com.msa.jewelry.local.product.dto.ProductWorkGradePolicyGroupDto;

import java.util.List;

public interface CustomProductWorkGradePolicyGroup {
    List<ProductWorkGradePolicyGroupDto.Response> findByWorkGradePolicyGroupByProductIdOrderById(Long productId);
}
