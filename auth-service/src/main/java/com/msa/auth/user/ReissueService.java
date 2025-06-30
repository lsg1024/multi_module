package com.msa.auth.user;


import com.msa.auth.redis.RedisRefreshTokenService;
import com.msacommon.global.jwt.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.stereotype.Service;

import static com.msa.auth.util.ValidationTokenUtil.*;

@Service
public class ReissueService {

    private final JwtUtil jwtUtil;
    private final RedisRefreshTokenService redisRefreshTokenService;

    public ReissueService(JwtUtil jwtUtil, RedisRefreshTokenService redisRefreshTokenService) {
        this.jwtUtil = jwtUtil;
        this.redisRefreshTokenService = redisRefreshTokenService;
    }

    public String[] reissueRefreshToken(String refreshToken, Long access_ttl, Long refresh_ttl) {

        String owner = jwtUtil.getOwner(refreshToken);
        String nickname = jwtUtil.getNickname(refreshToken);
        String category = jwtUtil.getCategory(refreshToken);

        validateOwner(owner);
        validateNickname(nickname);
        validateCategoryIsRefresh(category);

        try {
            jwtUtil.isExpired(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("로그인 유지시간 만료");
        }

        String id = jwtUtil.getId(refreshToken);
        String role = jwtUtil.getRole(refreshToken);

        String newAccessToken = jwtUtil.createJwt("access", id, owner, nickname, role, access_ttl);
        String newRefreshToken = jwtUtil.createJwt("refresh", id, owner, nickname, role, refresh_ttl);

        redisRefreshTokenService.updateNewToken(owner, nickname, newRefreshToken);

        return new String[]{newAccessToken, newRefreshToken};
    }
}
