package com.msa.jewelry.product.internal.product.repository.work_grade_policy_group.work_grade_policy;

import com.msa.jewelry.product.internal.product.entity.ProductWorkGradePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductWorkGradePolicyRepository extends JpaRepository<ProductWorkGradePolicy, Long>, CustomProductWorkGradePolicy {
}
