package com.msa.jewelry.local.stone.entity;

import com.msa.jewelry.local.stone.dto.StoneDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "STONE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "스톤(보석) 마스터 엔티티 — 다이아몬드/사파이어 등 보석 종류·모양·사이즈 조합 마스터")
public class Stone {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "STONE_ID")
    @Schema(description = "스톤 PK", example = "301")
    private Long stoneId;
    @Column(name = "STONE_NAME", unique = true)
    @Schema(description = "스톤명 (고유) — 보통 stoneType + stoneShape + stoneSize 로 합성", example = "다이아 라운드 0.3ct")
    private String stoneName; // stoneTypeName + stoneShapeName + stoneSize
    @Column(name = "STONE_NOTE")
    @Schema(description = "스톤 비고", example = "VS1 등급")
    private String stoneNote;
    @Column(name = "STONE_WEIGHT", precision = 5, scale = 2)
    @Schema(description = "스톤 무게 (캐럿 또는 그램)", example = "0.30")
    private BigDecimal stoneWeight;
    @Column(name = "STONE_PURCHASE_PRICE")
    @Schema(description = "스톤 매입 단가 (원)", example = "150000")
    private Integer stonePurchasePrice;
    @OneToMany(mappedBy = "stone", cascade = CascadeType.ALL, orphanRemoval = true)
    @Schema(description = "스톤 등급별 공임 정책 목록")
    private List<StoneWorkGradePolicy> gradePolicies = new ArrayList<>();

    @Builder
    public Stone(String stoneName, String stoneNote, BigDecimal stoneWeight, Integer stonePurchasePrice, List<StoneWorkGradePolicy> gradePolicies) {
        this.stoneName = stoneName;
        this.stoneNote = stoneNote;
        this.stoneWeight = stoneWeight;
        this.stonePurchasePrice = stonePurchasePrice;
        this.gradePolicies = gradePolicies;
    }

    public void addGradePolicy(StoneWorkGradePolicy policy) {
        gradePolicies.add(policy);
        policy.setStone(this);
    }

    public void updateStone(StoneDto stoneDto) {
        this.stoneName = stoneDto.getStoneName();
        this.stoneNote = stoneDto.getStoneNote();
        this.stoneWeight = new BigDecimal(stoneDto.getStoneWeight());
        this.stonePurchasePrice = stoneDto.getStonePurchasePrice();
    }
}
