package com.msa.product.local.product.repository.work_grade_policy_group.work_grade_policy;

import com.msa.product.local.product.entity.ProductWorkGradePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductWorkGradePolicyRepository extends JpaRepository<ProductWorkGradePolicy, Long>, CustomProductWorkGradePolicy {
}
