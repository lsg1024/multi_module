package com.msa.userserver.domain.entity;

import com.msa.common.global.domain.BaseTimeEntity;
import com.msa.common.global.domain.dto.UserDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE USERS SET DELETED = TRUE WHERE ID = ?")
public class Users extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USER_ID", nullable = false, length = 10)
    private String userId; // 사용자 아이디

    @Column(name = "PASSWORD", nullable = false, length = 100)
    private String password;

    @Column(name = "NICKNAME", nullable = false, length = 10)
    private String nickname; // 이름

    @Column(name = "TENANT_ID", nullable = false, length = 10)
    private String tenantId;

    @Column(name = "ROLE", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "STORE_ID")
    private Long storeId;  // 판매처 계정인 경우 연결된 Store ID

    @Column(name = "DELETED", nullable = false)
    private boolean deleted = false;

    @Builder
    public Users(String userId, String password, String nickname, String tenantId, Role role, Long storeId) {
        this.userId = userId;
        this.password = password;
        this.nickname = nickname;
        this.tenantId = tenantId;
        this.role = role;
        this.storeId = storeId;
    }

    public void updateInfo(UserDto.Update updateDto) {
        this.nickname = updateDto.getNickname();

        try {
            this.role = Role.valueOf(updateDto.getRole());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("올바르지 않은 권한 값입니다: " + updateDto.getRole());
        }
    }

    public void softDeleted() {
        this.deleted = true;
    }

    public void updatePassword(String password) {
        this.password = password;
    }
}

