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

    public String getTenantIdFromToken(String refreshToken) {
        return jwtUtil.getTenantId(refreshToken);
    }

    /**
     * refresh token에서 추출한 클레임으로 Redis 키를 구성하여 토큰을 삭제한다.
     *
     * *cross-tenant 재발급 시도가 감지되었을 때 서버 측 세션을 완전히 무효화하기 위해 호출된다.
     * 토큰 파싱 자체가 실패하더라도 예외를 전파하지 않고 안전하게 처리한다.
     *
     * @param refreshToken 삭제할 refresh token 문자열
     */
    public void deleteTokenByRefreshToken(String refreshToken) {
        try {
            String tenantId = jwtUtil.getTenantId(refreshToken);
            String nickname = jwtUtil.getNickname(refreshToken);
            String forward = jwtUtil.getForward(refreshToken);
            String device = jwtUtil.getDevice(refreshToken);

            String key = "refreshToken:" + tenantId + ":" + forward + ":" + device + ":" + nickname;
            redisRefreshTokenService.deleteByKey(key);
        } catch (Exception e) {
            // 토큰 파싱 실패 시에도 안전하게 처리
        }
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
