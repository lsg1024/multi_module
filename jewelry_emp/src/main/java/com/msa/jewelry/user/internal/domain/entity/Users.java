package com.msa.jewelry.user.internal.domain.entity;

import com.msa.common.global.domain.BaseTimeEntity;
import com.msa.common.global.domain.dto.UserDto;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "사용자(직원) 엔티티 — 운영자/판매처 계정. 실제 인증 토큰 발급은 외부 auth-service 가 담당하고, 본 엔티티는 사용자 프로필·권한·소속 정보를 보관한다.")
public class Users extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Schema(description = "사용자 PK", example = "1")
    private Long id;

    @Column(name = "USER_ID", nullable = false, length = 10)
    @Schema(description = "로그인 ID (사용자 아이디, 최대 10자)", example = "admin01")
    private String userId; // 사용자 아이디

    @Column(name = "PASSWORD", nullable = false, length = 100)
    @Schema(description = "암호화된 비밀번호 (응답 시에는 노출하지 말 것)", example = "$2a$10$abcdefg...")
    private String password;

    @Column(name = "NICKNAME", nullable = false, length = 10)
    @Schema(description = "사용자 표시명(이름/닉네임, 최대 10자)", example = "김관리")
    private String nickname; // 이름

    @Column(name = "TENANT_ID", nullable = false, length = 10)
    @Schema(description = "테넌트(고객사) 식별자 — 멀티테넌시 분리 키", example = "tenant01")
    private String tenantId;

    @Column(name = "ROLE", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    @Schema(description = "사용자 역할 (ADMIN/USER/GUEST/STORE)", example = "ADMIN")
    private Role role;

    @Column(name = "STORE_ID")
    @Schema(description = "판매처(STORE) 계정인 경우 연결된 매장 ID. ADMIN/USER 는 null.", example = "10")
    private Long storeId;  // 판매처 계정인 경우 연결된 Store ID

    @Column(name = "DELETED", nullable = false)
    @Schema(description = "소프트 삭제 플래그 — TRUE 이면 비활성 계정", example = "false")
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

    /**
     * 사용자 정보를 부분 업데이트한다.
     * null/빈 문자열로 전달된 필드는 기존 DB 값을 유지한다 (payload 누락 시 데이터 유실 방지).
     */
    public void updateInfo(UserDto.Update updateDto) {
        if (updateDto == null) {
            return;
        }
        if (updateDto.getNickname() != null && !updateDto.getNickname().isEmpty()) {
            this.nickname = updateDto.getNickname();
        }
        if (updateDto.getRole() != null && !updateDto.getRole().isEmpty()) {
            try {
                this.role = Role.valueOf(updateDto.getRole());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("올바르지 않은 권한 값입니다: " + updateDto.getRole());
            }
        }
    }

    public void softDeleted() {
        this.deleted = true;
    }

    public void updatePassword(String password) {
        this.password = password;
    }
}

