package com.msa.auth.user;


import com.msa.auth.redis.RedisRefreshTokenService;
import com.msa.common.global.jwt.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.stereotype.Service;

import static com.msa.auth.util.ValidationTokenUtil.*;

@Service
public class RefreshTokenService {

    private final JwtUtil jwtUtil;
    private final RedisRefreshTokenService redisRefreshTokenService;

    public RefreshTokenService(JwtUtil jwtUtil, RedisRefreshTokenService redisRefreshTokenService) {
        this.jwtUtil = jwtUtil;
        this.redisRefreshTokenService = redisRefreshTokenService;
    }

    public String[] reissueRefreshToken(String refreshToken, Long access_ttl, Long refresh_ttl) {

        String tenantId = jwtUtil.getTenantId(refreshToken);
        String nickname = jwtUtil.getNickname(refreshToken);
        String category = jwtUtil.getCategory(refreshToken);

        validateTenantId(tenantId);
        validateNickname(nickname);
        validateCategoryIsRefresh(category);

        try {
            jwtUtil.isExpired(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("로그인 유지시간 만료");
        }

        String id = jwtUtil.getId(refreshToken);
        String role = jwtUtil.getRole(refreshToken);
        String forward = jwtUtil.getForward(refreshToken);
        String device = jwtUtil.getDevice(refreshToken);

        String newAccessToken = jwtUtil.createJwt("access", id, tenantId, nickname, forward, device, role, access_ttl);
        String newRefreshToken = jwtUtil.createJwt("refresh", id, tenantId, nickname, forward, device, role, refresh_ttl);

        redisRefreshTokenService.updateNewToken(tenantId, forward, device, nickname, newRefreshToken);

        return new String[]{newAccessToken, newRefreshToken};
    }
}
