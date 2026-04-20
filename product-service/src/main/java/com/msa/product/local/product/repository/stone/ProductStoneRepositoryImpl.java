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
                        stone.stoneWeight,
                        stone.stonePurchasePrice,
                        productStone.mainStone,
                        productStone.includeStone,
                        productStone.includeQuantity,
                        productStone.includePrice,
                        productStone.stoneQuantity,
                        productStone.productStoneNote
                ))
                .from(productStone)
                .leftJoin(productStone.stone, stone)
                .where(productStone.product.productId.eq(productId))
                .fetch();

        // stoneId가 null인 항목 필터링 (마이그레이션으로 생성된 ProductStone은 stone FK가 없을 수 있음)
        List<Long> stoneIds = productStones.stream()
                .filter(dto -> dto.getStoneId() != null)
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

        // 4. stoneDtos에 정책 리스트 주입 (stoneId가 null인 항목은 빈 정책 리스트)
        productStones.forEach(dto -> {
            if (dto.getStoneId() != null) {
                Long stoneId = Long.parseLong(dto.getStoneId());
                dto.setStoneWorkGradePolicyDtos(policyMap.getOrDefault(stoneId, List.of()));
            } else {
                dto.setStoneWorkGradePolicyDtos(List.of());
            }
        });

        return productStones;
    }
}
