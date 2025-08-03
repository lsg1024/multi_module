package com.msa.common.global.domain.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

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
    public static class UserInfo {
        @Pattern(regexp = "^[A-Za-z0-9]+$", message = USERID_ERROR)
        @Size(min = 5, max = 10)
        private String userId;
        private String tenantId;
        private String nickname;
        private String role;

        @Builder
        public UserInfo(String userId, String tenantId, String nickname, String role) {
            this.userId = userId;
            this.tenantId = tenantId;
            this.nickname = nickname;
            this.role = role;
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

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {

        @Pattern(regexp = "^[A-Za-z0-9]+$", message = USERID_ERROR)
        @Size(min = 5, max = 10)
        private String userId;

        private String nickname;

        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[$@!%*#?&]).{8,16}$",
                message = PASSWORD_ERROR)
        private String password;

        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[$@!%*#?&]).{8,16}$",
                message = PASSWORD_ERROR)
        private String confirm_password;
    }

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update {
        private String id;

        @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$",
                message = "이름에 특수문자는 사용할 수 없습니다.")
        private String nickname;
        private String role;
    }

}
