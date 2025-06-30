package com.account.global.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

@Entity
@Table(name = "BUSINESS")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE BUSINESS SET DELETED = TRUE WHERE BUSINESS_ID = ?")
public class Business {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BUSINESS_ID")
    private Long businessId;
    @Column(name = "BUSINESS_NAME")
    private String businessName;
    @Column(name = "BUSINESS_OWNER_NAME")
    private String businessOwnerName;
    @Column(name = "BUSINESS_NUMBER_1")
    private String businessNumber1;
    @Column(name = "BUSINESS_NUMBER_2")
    private String businessNumber2;
    @Column(name = "DELETED")
    private boolean deleted = false;

    @Builder
    public Business(String businessName, String businessNumber1, String businessOwnerName, String businessNumber2) {
        this.businessName = businessName;
        this.businessNumber1 = businessNumber1;
        this.businessOwnerName = businessOwnerName;
        this.businessNumber2 = businessNumber2;
    }
}
