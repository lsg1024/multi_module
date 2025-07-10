package com.msa.userserver.domain.entity;

import com.msacommon.global.domain.BaseTimeEntity;
import com.msacommon.global.domain.dto.UserDto;
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

    @Column(name = "DELETED", nullable = false)
    private boolean deleted = false;

    @Builder
    public Users(String userId, String password, String nickname, String tenantId, Role role) {
        this.userId = userId;
        this.password = password;
        this.nickname = nickname;
        this.tenantId = tenantId;
        this.role = role;
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

}
