package com.msa.account.domain.store.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

@Entity
@Table(name = "ADDITIONAL_OPTION")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE ADDITIONAL_OPTION SET DELETED = TRUE WHERE OPTION_ID = ?")
public class AdditionalOption {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OPTION_ID")
    private String optionID;
    @Column(name = "OPTION_APPLY_PAST_SALES")
    private String optionApplyPastSales;
    @Column(name = "OPTION_LIMIT_DAY")
    private String optionLimitDay;
    @Column(name = "OPTION_MATERIAL")
    private String optionMaterial;
    private boolean deleted = false;

    @Builder
    public AdditionalOption(String optionApplyPastSales, String optionLimitDay, String optionMaterial) {
        this.optionApplyPastSales = optionApplyPastSales;
        this.optionLimitDay = optionLimitDay;
        this.optionMaterial = optionMaterial;
    }
}
