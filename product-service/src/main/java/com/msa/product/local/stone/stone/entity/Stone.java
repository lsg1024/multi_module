package com.msa.product.local.stone.stone.entity;

import com.msa.product.local.stone.stone.dto.StoneDto;
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
public class Stone {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "STONE_ID")
    private Long stoneId;
    @Column(name = "STONE_NAME", unique = true)
    private String stoneName; // stoneTypeName + stoneShapeName + stoneSize
    @Column(name = "STONE_NOTE")
    private String stoneNote;
    @Column(name = "STONE_WEIGHT", precision = 5, scale = 2)
    private BigDecimal stoneWeight;
    @Column(name = "STONE_PURCHASE_PRICE")
    private Integer stonePurchasePrice;
    @OneToMany(mappedBy = "stone", cascade = CascadeType.ALL, orphanRemoval = true)
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
