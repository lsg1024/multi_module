package com.msa.auth.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;


@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private final static String USERID_ERROR = "영어/숫자만 가능합니다. 글자제한(5 ~ 10)";
    private final static String PASSWORD_ERROR = "비밀번호는 영문자, 숫자, 특수문자를 포함한 8~16자리여야 합니다.";

    private String userId;
    private String owner;
    private String nickname;
    private String password;
    private String role;
    private String category;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String userId;
        private String tenantId;
        private String nickname;
        private String role;

        public Collection<? extends GrantedAuthority> getAuthorities() {
            return Collections.singletonList(new SimpleGrantedAuthority(role));
        }
    }

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Login {

        @Pattern(regexp = "^[A-Za-z0-9]+$", message = USERID_ERROR)
        @Size(min = 5, max = 10)
        private String userId;

        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[$@!%*#?&]).{8,16}$",
                message = PASSWORD_ERROR)
        private String password;
    }

}
