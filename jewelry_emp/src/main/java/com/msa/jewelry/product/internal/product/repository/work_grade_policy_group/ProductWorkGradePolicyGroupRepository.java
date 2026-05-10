package com.msa.jewelry.product.internal.product.repository.work_grade_policy_group;

import com.msa.jewelry.product.internal.product.entity.ProductWorkGradePolicyGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductWorkGradePolicyGroupRepository extends JpaRepository<ProductWorkGradePolicyGroup, Long> {
    @Query("select g from ProductWorkGradePolicyGroup g " +
            "left join fetch g.color " +
            "left join fetch g.product " +
            "left join fetch g.gradePolicies " +
            "where g.productWorkGradePolicyGroupId in :groupIds " +
            "and g.product.productId = :productId")
    List<ProductWorkGradePolicyGroup> findAllWithDetailsByGroupIds(@Param("groupIds") List<Long> groupIds, @Param("productId") Long productId);

}
