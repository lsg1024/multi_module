package com.msacommon.local.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

@Entity
@Table(name = "BUSINESS")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE BUSINESS SET DELETED = TRUE WHERE BUSINESS_ID: ?")
public class Business {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BUSINESS_ID")
    private Long businessId;
    @Column(name = "BUSINESS_NAME")
    private String businessName;
    @Column(name = "BUSINESS_NUMBER")
    private String businessNumber;
    @Column(name = "BUSINESS_OWNER_NAME")
    private String businessOwnerName;
    @Column(name = "BUSINESS_OWNER_NUMBER")
    private String businessOwnerNumber;
    @Column(name = "DELETED")
    private boolean deleted = false;

    @Builder
    public Business(String businessName, String businessNumber, String businessOwnerName, String businessOwnerNumber) {
        this.businessName = businessName;
        this.businessNumber = businessNumber;
        this.businessOwnerName = businessOwnerName;
        this.businessOwnerNumber = businessOwnerNumber;
    }
}
