package com.msa.jewelry.local.user.service;

import com.msa.common.global.domain.dto.UserDto;
import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.tenant.TenantContext;
import com.msa.common.global.util.AuthorityUserRoleUtil;
import com.msa.jewelry.local.user.entity.Role;
import com.msa.jewelry.local.user.entity.Users;
import com.msa.jewelry.local.user.exception.UserNotFoundException;
import com.msa.jewelry.local.user.repository.UsersRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UsersService 단위 테스트")
class UsersServiceTest {

    private static final String TOKEN     = "Bearer test-token";
    private static final String TENANT_ID = "tenant-001";
    private static final Long   USER_PK   = 1L;
    private static final String USER_ID   = "tester01";

    @Mock JwtUtil jwtUtil;
    @Mock BCryptPasswordEncoder encoder;
    @Mock AuthorityUserRoleUtil authorityUserRoleUtil;
    @Mock UsersRepository usersRepository;

    @InjectMocks
    UsersService usersService;

    @BeforeEach
    void setTenant() {
        TenantContext.setTenant(TENANT_ID);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("정상 — Repository.save 호출")
        void 정상생성() {
            UserDto.Create dto = new UserDto.Create(USER_ID, "닉네임", "Password1!", "Password1!", "USER", 10L);
            given(usersRepository.existsByUserId(USER_ID)).willReturn(false);
            given(encoder.encode("Password1!")).willReturn("encoded-pw");

            usersService.createUser(dto);

            ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
            verify(usersRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
            assertThat(captor.getValue().getPassword()).isEqualTo("encoded-pw");
            assertThat(captor.getValue().getRole()).isEqualTo(Role.USER);
            assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_ID);
            assertThat(captor.getValue().getStoreId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Role null → 기본값 GUEST 로 저장")
        void role_null이면_GUEST() {
            UserDto.Create dto = new UserDto.Create(USER_ID, "닉네임", "Password1!", "Password1!", null, null);
            given(usersRepository.existsByUserId(USER_ID)).willReturn(false);
            given(encoder.encode(anyString())).willReturn("encoded");

            usersService.createUser(dto);

            ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
            verify(usersRepository).save(captor.capture());
            assertThat(captor.getValue().getRole()).isEqualTo(Role.GUEST);
        }

        @Test
        @DisplayName("비밀번호 불일치 → IllegalArgumentException, save 호출 안 함")
        void 비밀번호_불일치() {
            UserDto.Create dto = new UserDto.Create(USER_ID, "닉네임", "Password1!", "Different2@", "USER", null);

            assertThatThrownBy(() -> usersService.createUser(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("입력값이 다릅니다");

            verify(usersRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 존재하는 userId → IllegalArgumentException")
        void 중복_userId() {
            UserDto.Create dto = new UserDto.Create(USER_ID, "닉네임", "Password1!", "Password1!", "USER", null);
            given(usersRepository.existsByUserId(USER_ID)).willReturn(true);

            assertThatThrownBy(() -> usersService.createUser(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 존재하는 아이디");

            verify(usersRepository, never()).save(any());
        }

        @Test
        @DisplayName("잘못된 Role 값 → IllegalArgumentException")
        void 잘못된_Role() {
            UserDto.Create dto = new UserDto.Create(USER_ID, "닉네임", "Password1!", "Password1!", "NOT_A_REAL_ROLE", null);
            given(usersRepository.existsByUserId(USER_ID)).willReturn(false);

            assertThatThrownBy(() -> usersService.createUser(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("올바르지 않은 권한");
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getUserInfo")
    class GetUserInfo {

        @Test
        @DisplayName("정상 — userId/nickname 반환")
        void 정상조회() {
            Users user = mock(Users.class);
            given(user.getUserId()).willReturn(USER_ID);
            given(user.getNickname()).willReturn("홍길동");

            given(jwtUtil.getId(TOKEN)).willReturn(USER_PK.toString());
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);
            given(usersRepository.findByIdAndTenantId(USER_PK, TENANT_ID))
                    .willReturn(Optional.of(user));

            UserDto.UserInfo info = usersService.getUserInfo(TOKEN);

            assertThat(info.getUserId()).isEqualTo(USER_ID);
            assertThat(info.getNickname()).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("토큰의 사용자가 존재하지 않으면 UserNotFoundException")
        void 토큰_불일치() {
            given(jwtUtil.getId(TOKEN)).willReturn(USER_PK.toString());
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);
            given(usersRepository.findByIdAndTenantId(USER_PK, TENANT_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> usersService.getUserInfo(TOKEN))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("사용자 정보 불일치");
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateUserInfo")
    class UpdateUserInfo {

        @Test
        @DisplayName("본인 — 닉네임 수정 정상")
        void 본인_수정() {
            // checkToken 통과용
            Users currentUser = mock(Users.class);
            given(jwtUtil.getId(TOKEN)).willReturn(USER_PK.toString());
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);
            given(usersRepository.findByIdAndTenantId(USER_PK, TENANT_ID))
                    .willReturn(Optional.of(currentUser));

            // 대상 사용자
            Users target = mock(Users.class);
            given(target.getUserId()).willReturn(USER_ID);
            given(target.getRole()).willReturn(Role.USER);
            given(usersRepository.findById(2L)).willReturn(Optional.of(target));

            given(authorityUserRoleUtil.isSelf(USER_ID, TOKEN)).willReturn(true);
            given(authorityUserRoleUtil.isAdmin(TOKEN)).willReturn(false);

            UserDto.Update dto = new UserDto.Update("2", "새이름", null);
            usersService.updateUserInfo(TOKEN, dto);

            verify(target).updateInfo(dto);
        }

        @Test
        @DisplayName("본인 아님 & 관리자 아님 → IllegalArgumentException(권한 거부)")
        void 권한_거부() {
            Users currentUser = mock(Users.class);
            given(jwtUtil.getId(TOKEN)).willReturn(USER_PK.toString());
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);
            given(usersRepository.findByIdAndTenantId(USER_PK, TENANT_ID))
                    .willReturn(Optional.of(currentUser));

            Users target = mock(Users.class);
            given(target.getUserId()).willReturn("other-user");
            given(usersRepository.findById(2L)).willReturn(Optional.of(target));

            given(authorityUserRoleUtil.isSelf("other-user", TOKEN)).willReturn(false);
            given(authorityUserRoleUtil.isAdmin(TOKEN)).willReturn(false);

            UserDto.Update dto = new UserDto.Update("2", "새이름", null);

            assertThatThrownBy(() -> usersService.updateUserInfo(TOKEN, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("권한이 없습니다");

            verify(target, never()).updateInfo(any());
        }

        @Test
        @DisplayName("본인 + Role 변경 시도(관리자 아님) → IllegalArgumentException")
        void 본인_Role변경_관리자아님() {
            Users currentUser = mock(Users.class);
            given(jwtUtil.getId(TOKEN)).willReturn(USER_PK.toString());
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);
            given(usersRepository.findByIdAndTenantId(USER_PK, TENANT_ID))
                    .willReturn(Optional.of(currentUser));

            Users target = mock(Users.class);
            given(target.getUserId()).willReturn(USER_ID);
            given(target.getRole()).willReturn(Role.USER);
            given(usersRepository.findById(2L)).willReturn(Optional.of(target));

            given(authorityUserRoleUtil.isSelf(USER_ID, TOKEN)).willReturn(true);
            given(authorityUserRoleUtil.isAdmin(TOKEN)).willReturn(false);

            UserDto.Update dto = new UserDto.Update("2", "새이름", "ADMIN");

            assertThatThrownBy(() -> usersService.updateUserInfo(TOKEN, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("관리자만 회원의 권한을 변경");

            verify(target, never()).updateInfo(any());
        }

        @Test
        @DisplayName("관리자 + Role 변경 — updateInfo 호출")
        void 관리자_Role변경() {
            Users currentUser = mock(Users.class);
            given(jwtUtil.getId(TOKEN)).willReturn(USER_PK.toString());
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);
            given(usersRepository.findByIdAndTenantId(USER_PK, TENANT_ID))
                    .willReturn(Optional.of(currentUser));

            Users target = mock(Users.class);
            given(target.getUserId()).willReturn("other-user");
            given(target.getRole()).willReturn(Role.USER);
            given(usersRepository.findById(2L)).willReturn(Optional.of(target));

            given(authorityUserRoleUtil.isSelf("other-user", TOKEN)).willReturn(false);
            given(authorityUserRoleUtil.isAdmin(TOKEN)).willReturn(true);

            UserDto.Update dto = new UserDto.Update("2", "새이름", "ADMIN");
            usersService.updateUserInfo(TOKEN, dto);

            verify(target).updateInfo(dto);
        }

        @Test
        @DisplayName("대상 사용자 없음 → UserNotFoundException")
        void 대상_없음() {
            Users currentUser = mock(Users.class);
            given(jwtUtil.getId(TOKEN)).willReturn(USER_PK.toString());
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);
            given(usersRepository.findByIdAndTenantId(USER_PK, TENANT_ID))
                    .willReturn(Optional.of(currentUser));
            given(usersRepository.findById(99L)).willReturn(Optional.empty());

            UserDto.Update dto = new UserDto.Update("99", "x", null);

            assertThatThrownBy(() -> usersService.updateUserInfo(TOKEN, dto))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("대상 사용자를 찾을 수 없습니다");
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("deletedUser")
    class DeletedUser {

        @Test
        @DisplayName("정상 — softDeleted 호출")
        void 정상삭제() {
            Users user = mock(Users.class);
            given(jwtUtil.getId(TOKEN)).willReturn(USER_PK.toString());
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);
            given(usersRepository.findByIdAndTenantId(USER_PK, TENANT_ID))
                    .willReturn(Optional.of(user));

            usersService.deletedUser(TOKEN);

            verify(user).softDeleted();
        }

        @Test
        @DisplayName("토큰 사용자 없음 → UserNotFoundException")
        void 사용자_없음() {
            given(jwtUtil.getId(TOKEN)).willReturn(USER_PK.toString());
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);
            given(usersRepository.findByIdAndTenantId(USER_PK, TENANT_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> usersService.deletedUser(TOKEN))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsers {

        @Test
        @DisplayName("본인 제외 + PK 오름차순 정렬")
        void 본인제외_정렬() {
            Users me = mock(Users.class);
            // 본인 식별: jwtUtil.getId 가 반환하는 값과 u.getUserId() 가 동일해야 필터됨
            String selfKey = "self-key";
            given(jwtUtil.getId(TOKEN)).willReturn(selfKey);
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);
            // checkToken 은 jwtUtil.getId 를 Long parse 하므로, 별도로 parse 가능한 값으로 stubbing.
            // 실제로는 jwtUtil.getId 가 두 번 호출되는데, 한 번은 Long.parseLong 으로 변환됨.
            // 따라서 selfKey 가 "1" 이어야 한다.
            given(jwtUtil.getId(TOKEN)).willReturn("1");
            given(usersRepository.findByIdAndTenantId(1L, TENANT_ID))
                    .willReturn(Optional.of(me));

            // findAll 의 결과: 본인(userId="1") + 다른 2명
            Users me2 = mock(Users.class);
            given(me2.getUserId()).willReturn("1"); // jwtUtil.getId 와 동일 → 필터됨
            given(me2.getId()).willReturn(USER_PK);
            given(me2.getTenantId()).willReturn(TENANT_ID);
            given(me2.getNickname()).willReturn("나");
            given(me2.getRole()).willReturn(Role.ADMIN);
            given(me2.getStoreId()).willReturn(null);

            Users a = mock(Users.class);
            given(a.getUserId()).willReturn("user-A");
            given(a.getId()).willReturn(5L);
            given(a.getTenantId()).willReturn(TENANT_ID);
            given(a.getNickname()).willReturn("A");
            given(a.getRole()).willReturn(Role.USER);
            given(a.getStoreId()).willReturn(null);

            Users b = mock(Users.class);
            given(b.getUserId()).willReturn("user-B");
            given(b.getId()).willReturn(2L);
            given(b.getTenantId()).willReturn(TENANT_ID);
            given(b.getNickname()).willReturn("B");
            given(b.getRole()).willReturn(Role.USER);
            given(b.getStoreId()).willReturn(null);

            given(usersRepository.findAll()).willReturn(List.of(me2, a, b));

            List<UserDto.UserInfo> result = usersService.getAllUsers(TOKEN);

            // 본인 제외 + PK 문자열 오름차순 (서비스가 Integer.parseInt(userId) 로 정렬)
            // map 단계에서 userId 는 u.getId().toString() 으로 채워짐
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getUserId()).isEqualTo("2"); // b (PK=2)
            assertThat(result.get(1).getUserId()).isEqualTo("5"); // a (PK=5)
        }

        @Test
        @DisplayName("findAll 빈 결과 → 빈 리스트")
        void 빈결과() {
            Users me = mock(Users.class);
            given(jwtUtil.getId(TOKEN)).willReturn(USER_PK.toString());
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);
            given(usersRepository.findByIdAndTenantId(USER_PK, TENANT_ID))
                    .willReturn(Optional.of(me));
            given(usersRepository.findAll()).willReturn(Collections.emptyList());

            assertThat(usersService.getAllUsers(TOKEN)).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("정상 — USER 역할 로그인 성공")
        void 정상() {
            Users user = mock(Users.class);
            given(user.getId()).willReturn(USER_PK);
            given(user.getNickname()).willReturn("길동");
            given(user.getTenantId()).willReturn(TENANT_ID);
            given(user.getRole()).willReturn(Role.USER);
            given(user.getStoreId()).willReturn(null);
            given(user.getPassword()).willReturn("encoded-pw");

            given(usersRepository.findByUserId(USER_ID)).willReturn(Optional.of(user));
            given(encoder.matches("Password1!", "encoded-pw")).willReturn(true);

            UserDto.Login req = new UserDto.Login(USER_ID, "Password1!");

            UserDto.UserInfo info = usersService.login(req);

            assertThat(info.getUserId()).isEqualTo(USER_PK.toString());
            assertThat(info.getNickname()).isEqualTo("길동");
            assertThat(info.getRole()).isEqualTo(Role.USER.toString());
        }

        @Test
        @DisplayName("사용자 없음 → UserNotFoundException")
        void 사용자_없음() {
            given(usersRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

            UserDto.Login req = new UserDto.Login(USER_ID, "Password1!");

            assertThatThrownBy(() -> usersService.login(req))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("유저 정보가 없습니다");
        }

        @Test
        @DisplayName("비밀번호 불일치 → IllegalArgumentException")
        void 비밀번호_불일치() {
            Users user = mock(Users.class);
            given(user.getPassword()).willReturn("encoded-pw");
            given(usersRepository.findByUserId(USER_ID)).willReturn(Optional.of(user));
            given(encoder.matches("WrongPw1!", "encoded-pw")).willReturn(false);

            UserDto.Login req = new UserDto.Login(USER_ID, "WrongPw1!");

            assertThatThrownBy(() -> usersService.login(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("아이디와 비밀번호를 확인");
        }

        @Test
        @DisplayName("GUEST — 권한 부족 IllegalArgumentException")
        void GUEST_차단() {
            Users user = mock(Users.class);
            given(user.getId()).willReturn(USER_PK);
            given(user.getNickname()).willReturn("게스트");
            given(user.getTenantId()).willReturn(TENANT_ID);
            given(user.getRole()).willReturn(Role.GUEST);
            given(user.getStoreId()).willReturn(null);
            given(user.getPassword()).willReturn("encoded-pw");

            given(usersRepository.findByUserId(USER_ID)).willReturn(Optional.of(user));
            given(encoder.matches("Password1!", "encoded-pw")).willReturn(true);

            UserDto.Login req = new UserDto.Login(USER_ID, "Password1!");

            assertThatThrownBy(() -> usersService.login(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("권한이 부족");
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updatePassword")
    class UpdatePassword {

        @Test
        @DisplayName("정상 — entity.updatePassword 호출")
        void 정상() {
            Users user = mock(Users.class);
            given(user.getPassword()).willReturn("old-encoded");
            given(jwtUtil.getId(TOKEN)).willReturn(USER_PK.toString());
            given(usersRepository.findById(USER_PK)).willReturn(Optional.of(user));

            given(encoder.matches("OldPass1!", "old-encoded")).willReturn(true);
            given(encoder.matches("NewPass2@", "old-encoded")).willReturn(false);
            given(encoder.encode("NewPass2@")).willReturn("new-encoded");

            UserDto.Password dto = new UserDto.Password("OldPass1!", "NewPass2@", "NewPass2@");

            usersService.updatePassword(TOKEN, dto);

            verify(user).updatePassword("new-encoded");
        }

        @Test
        @DisplayName("사용자 없음 → UserNotFoundException")
        void 사용자_없음() {
            given(jwtUtil.getId(TOKEN)).willReturn(USER_PK.toString());
            given(usersRepository.findById(USER_PK)).willReturn(Optional.empty());

            UserDto.Password dto = new UserDto.Password("OldPass1!", "NewPass2@", "NewPass2@");

            assertThatThrownBy(() -> usersService.updatePassword(TOKEN, dto))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("기존 비밀번호 불일치 → IllegalArgumentException")
        void 기존_비밀번호_불일치() {
            Users user = mock(Users.class);
            given(user.getPassword()).willReturn("old-encoded");
            given(jwtUtil.getId(TOKEN)).willReturn(USER_PK.toString());
            given(usersRepository.findById(USER_PK)).willReturn(Optional.of(user));
            given(encoder.matches("WrongOld!", "old-encoded")).willReturn(false);

            UserDto.Password dto = new UserDto.Password("WrongOld!", "NewPass2@", "NewPass2@");

            assertThatThrownBy(() -> usersService.updatePassword(TOKEN, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("기존 비밀번호가 일치하지 않습니다");

            verify(user, never()).updatePassword(any());
        }

        @Test
        @DisplayName("새 비밀번호 = 기존 비밀번호 → IllegalArgumentException")
        void 동일_비밀번호() {
            Users user = mock(Users.class);
            given(user.getPassword()).willReturn("old-encoded");
            given(jwtUtil.getId(TOKEN)).willReturn(USER_PK.toString());
            given(usersRepository.findById(USER_PK)).willReturn(Optional.of(user));
            given(encoder.matches("Same1234!", "old-encoded")).willReturn(true);

            UserDto.Password dto = new UserDto.Password("Same1234!", "Same1234!", "Same1234!");

            assertThatThrownBy(() -> usersService.updatePassword(TOKEN, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("기존 비밀번호와 동일한 비밀번호");

            verify(user, never()).updatePassword(any());
        }

        @Test
        @DisplayName("새 비밀번호 ≠ 확인 비밀번호 → IllegalArgumentException")
        void 확인_불일치() {
            Users user = mock(Users.class);
            given(user.getPassword()).willReturn("old-encoded");
            given(jwtUtil.getId(TOKEN)).willReturn(USER_PK.toString());
            given(usersRepository.findById(USER_PK)).willReturn(Optional.of(user));
            given(encoder.matches("OldPass1!", "old-encoded")).willReturn(true);
            given(encoder.matches("NewPass2@", "old-encoded")).willReturn(false);

            UserDto.Password dto = new UserDto.Password("OldPass1!", "NewPass2@", "Different3#");

            assertThatThrownBy(() -> usersService.updatePassword(TOKEN, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("새 비밀번호 확인이 일치하지 않습니다");

            verify(user, never()).updatePassword(any());
        }
    }
}
