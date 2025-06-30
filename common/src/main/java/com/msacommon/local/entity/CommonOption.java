package com.msacommon.local.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

@Entity
@Table(name = "COMMON_OPTION")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE COMMON_OPTION SET DELETED = TRUE WHERE COMMON_OPTION_ID: ?")
public class CommonOption {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COMMON_OPTIONS_ID")
    private Long commonOptionId;
    private String commonOptionGoldLoss;
    private String commonOptionTradeType;
    private String commonOptionLevel;
    private String commonOptionManager;
    private boolean deleted = false;

    @Builder
    public CommonOption(String commonOptionGoldLoss, String commonOptionTradeType, String commonOptionLevel, String commonOptionManager) {
        this.commonOptionGoldLoss = commonOptionGoldLoss;
        this.commonOptionTradeType = commonOptionTradeType;
        this.commonOptionLevel = commonOptionLevel;
        this.commonOptionManager = commonOptionManager;
    }
}
