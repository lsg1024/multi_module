package com.msa.product.local.product.repository.work_grade_policy_group;

import com.msa.product.local.product.dto.*;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.msa.product.local.color.entity.QColor.color;
import static com.msa.product.local.product.entity.QProductWorkGradePolicy.productWorkGradePolicy;
import static com.msa.product.local.product.entity.QProductWorkGradePolicyGroup.productWorkGradePolicyGroup;

@Repository
public class ProductWorkGradePolicyRepositoryGroupImpl implements CustomProductWorkGradePolicyGroup {

    private final JPAQueryFactory query;

    public ProductWorkGradePolicyRepositoryGroupImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public List<ProductWorkGradePolicyGroupDto.Response> findByWorkGradePolicyGroupByProductId(Long productId) {

        List<ProductWorkGradePolicyGroupInfo> groups = query
                .select(new QProductWorkGradePolicyGroupInfo(
                        productWorkGradePolicyGroup.productWorkGradePolicyGroupId,
                        productWorkGradePolicyGroup.productPurchasePrice,
                        color.colorName,
                        productWorkGradePolicyGroup.productWorkGradePolicyGroupDefault,
                        productWorkGradePolicyGroup.note
                ))
                .from(productWorkGradePolicyGroup)
                .leftJoin(productWorkGradePolicyGroup.color, color)
                .where(productWorkGradePolicyGroup.product.productId.eq(productId))
                .fetch();

        // 2. 그룹 ID 추출
        List<Long> groupIds = groups.stream()
                .map(ProductWorkGradePolicyGroupInfo::getGroupId)
                .toList();

        List<ProductWorkGradePolicyDto.Response> policyDtos = query
                .select(new QProductWorkGradePolicyDto_Response(
                        productWorkGradePolicy.productWorkGradePolicyId.stringValue(), // String 변환
                        productWorkGradePolicy.grade.stringValue(),
                        productWorkGradePolicy.laborCost,
                        productWorkGradePolicy.workGradePolicyGroup.productWorkGradePolicyGroupId
                ))
                .from(productWorkGradePolicy)
                .where(productWorkGradePolicy.workGradePolicyGroup.productWorkGradePolicyGroupId.in(groupIds))
                .groupBy()
                .fetch();


        // 4. 그룹별로 정책 묶기
        Map<Long, List<ProductWorkGradePolicyDto.Response>> policyMap = policyDtos.stream()
                .collect(Collectors.groupingBy(ProductWorkGradePolicyDto.Response::getGroupId));

        // 5. DTO 조합해서 반환
        return groups.stream()
                .map(group -> ProductWorkGradePolicyGroupDto.Response.builder()
                        .productGroupId(group.getGroupId().toString())
                        .productPurchasePrice(group.getProductPurchasePrice())
                        .colorName(group.getColorName())
                        .defaultProductPolicy(group.isDefaultProductPolicy())
                        .gradePolicyDtos(policyMap.getOrDefault(group.getGroupId(), List.of()))
                        .note(group.getNote())
                        .build())
                .toList();
    }

}
