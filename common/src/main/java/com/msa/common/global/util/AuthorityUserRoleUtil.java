package com.msa.common.global.util;

import com.msa.common.global.jwt.JwtUtil;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AuthorityUserRoleUtil {

    private static final String ADMIN = "ADMIN";
    private static final String USER = "USER";
    private static final String GUEST = "GUEST";

    private final JwtUtil jwtUtil;

    public AuthorityUserRoleUtil(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public boolean verification(String token) {
        String role = jwtUtil.getRole(token);

        boolean isAdmin = Objects.equals(role, ADMIN);
        boolean isUser = Objects.equals(role, USER);

        return isAdmin || isUser;
    }

    public boolean isAdmin(String token) {
        String role = jwtUtil.getRole(token);

        return Objects.equals(role, ADMIN);
    }
}
