package com.msa.account.global.domain.dto.util;

import com.msa.account.local.factory.entity.Factory;
import com.msa.account.local.store.entity.Store;
import com.msacommon.global.jwt.JwtUtil;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AuthorityUserRoleUtil {

    private static final String ADMIN = "ADMIN";
    private static final String GUEST = "GUEST";

    private final JwtUtil jwtUtil;

    public AuthorityUserRoleUtil(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public boolean storeVerification(String token, Store entity) {
        String role = jwtUtil.getRole(token);
        String userId = jwtUtil.getId(token);

        boolean isAdmin = Objects.equals(role, ADMIN);
        boolean isOwner = Objects.equals(entity.getCreatedBy(), userId);
        boolean isNotGuest = !Objects.equals(role, GUEST);

        return isAdmin || (isOwner && isNotGuest);
    }

    public boolean factoryVerification(String token, Factory entity) {
        String role = jwtUtil.getRole(token);
        String userId = jwtUtil.getId(token);

        boolean isAdmin = Objects.equals(role, ADMIN);
        boolean isOwner = Objects.equals(entity.getCreatedBy(), userId);
        boolean isNotGuest = !Objects.equals(role, GUEST);

        return isAdmin || (isOwner && isNotGuest);
    }
}
