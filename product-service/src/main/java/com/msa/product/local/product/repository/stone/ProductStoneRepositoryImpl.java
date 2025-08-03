package com.msa.product.local.product.repository.stone;

import com.msa.product.local.product.dto.ProductStoneDto;
import com.msa.product.local.product.dto.QProductStoneDto_Response;
import com.msa.product.local.stone.stone.dto.StoneWorkGradePolicyDto;
import com.msa.product.local.stone.stone.entity.StoneWorkGradePolicy;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.msa.product.local.product.entity.QProductStone.productStone;
import static com.msa.product.local.stone.stone.entity.QStone.stone;
import static com.msa.product.local.stone.stone.entity.QStoneWorkGradePolicy.stoneWorkGradePolicy;

public class ProductStoneRepositoryImpl implements CustomProductStoneRepository {

    private final JPAQueryFactory query;

    public ProductStoneRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public List<ProductStoneDto.Response> findProductStones(Long productId) {
        List<ProductStoneDto.Response> productStones = query
                .select(new QProductStoneDto_Response(
                        productStone.productStoneId.stringValue(),
                        stone.stoneId.stringValue(),
                        stone.stoneName,
                        productStone.productStoneMain,
                        productStone.includeQuantity,
                        productStone.includeWeight,
                        productStone.includeLabor,
                        productStone.stoneQuantity
                ))
                .from(productStone)
                .join(productStone.stone, stone)
                .where(productStone.product.productId.eq(productId))
                .fetch();

        List<Long> stoneIds = productStones.stream()
                .map(dto -> Long.parseLong(dto.getStoneId()))
                .toList();

        if (stoneIds.isEmpty()) {
            return productStones;
        }

        List<StoneWorkGradePolicy> policies = query
                .selectFrom(stoneWorkGradePolicy)
                .where(stoneWorkGradePolicy.stone.stoneId.in(stoneIds))
                .fetch();

        Map<Long, List<StoneWorkGradePolicyDto.Response>> policyMap = policies.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getStone().getStoneId(),
                        Collectors.mapping(StoneWorkGradePolicyDto.Response::fromEntity, Collectors.toList())
                ));

        // 4. stoneDtos에 정책 리스트 주입
        productStones.forEach(dto -> {
            Long stoneId = Long.parseLong(dto.getStoneId());
            dto.setStoneWorkGradePolicyDtos(policyMap.getOrDefault(stoneId, List.of()));
        });

        return productStones;
    }
}
