package com.msa.product.local.product.repository.work_grade_policy_group;

import com.msa.product.local.product.dto.ProductWorkGradePolicyDto;
import com.msa.product.local.product.dto.ProductWorkGradePolicyGroupDto;
import com.msa.product.local.product.dto.QProductWorkGradePolicyDto_Response;
import com.msa.product.local.product.dto.QProductWorkGradePolicyGroupDto_Response;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.Collections;
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

        List<ProductWorkGradePolicyGroupDto.Response> groups = query
                .select(new QProductWorkGradePolicyGroupDto_Response(
                        productWorkGradePolicyGroup.productWorkGradePolicyGroupId.stringValue(),
                        productWorkGradePolicyGroup.productPurchasePrice,
                        color.colorId.stringValue(),
                        color.colorName,
                        productWorkGradePolicyGroup.productWorkGradePolicyGroupDefault,
                        Expressions.constant(Collections.emptyList()),
                        productWorkGradePolicyGroup.note
                ))
                .from(productWorkGradePolicyGroup)
                .leftJoin(productWorkGradePolicyGroup.color, color)
                .where(productWorkGradePolicyGroup.product.productId.eq(productId))
                .fetch();

        List<Long> groupIds = groups.stream()
                .map(ProductWorkGradePolicyGroupDto.Response::getProductGroupId)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return Long.parseLong(s);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
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
                .fetch();

        // 4. 그룹별로 정책 묶기
        Map<Long, List<ProductWorkGradePolicyDto.Response>> policyMap = policyDtos.stream()
                .collect(Collectors.groupingBy(ProductWorkGradePolicyDto.Response::getGroupId));

        // 5. DTO 조합해서 반환
        return groups.stream()
                .map(group -> ProductWorkGradePolicyGroupDto.Response.builder()
                        .productGroupId(group.getProductGroupId())
                        .productPurchasePrice(group.getProductPurchasePrice())
                        .colorId(group.getColorId())
                        .colorName(group.getColorName())
                        .defaultProductPolicy(group.isDefaultProductPolicy())
                        .gradePolicyDtos(policyMap.getOrDefault(Long.parseLong(group.getProductGroupId()), List.of()))
                        .note(group.getNote())
                        .build())
                .toList();
    }

}
