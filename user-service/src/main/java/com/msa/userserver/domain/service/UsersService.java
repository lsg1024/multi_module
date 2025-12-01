package com.msa.userserver.domain.service;


import com.msa.userserver.domain.respository.UsersRepository;
import com.msa.userserver.domain.entity.Role;
import com.msa.userserver.domain.entity.Users;
import com.msa.userserver.exception.UserNotFoundException;
import com.msa.common.global.domain.dto.UserDto;
import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class UsersService {

    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder;
    private final UsersRepository usersRepository;

    public UsersService(JwtUtil jwtUtil, BCryptPasswordEncoder encoder, UsersRepository usersRepository) {
        this.jwtUtil = jwtUtil;
        this.encoder = encoder;
        this.usersRepository = usersRepository;
    }

    //유저 생성
    public void createUser(final UserDto.Create userDto) {
        String tenantId = TenantContext.getTenant();

        if (!userDto.getPassword().equals(userDto.getConfirm_password())) {
            throw new IllegalArgumentException("입력값이 다릅니다.");
        }

        boolean existsByUserId = usersRepository.existsByUserId(userDto.getUserId());
        if (existsByUserId) {
            throw new IllegalArgumentException("이미 존재하는 아이디 입니다.");
        }

        final Users user = Users.builder()
                .userId(userDto.getUserId())
                .tenantId(tenantId)
                .password(encoder.encode(userDto.getPassword()))
                .nickname(userDto.getNickname())
                .role(Role.GUEST)
                .build();

        usersRepository.save(user);
    }

    //유저 정보
    @Transactional(readOnly = true)
    public UserDto.UserInfo getUserInfo(String accessToken) {
        Users users = checkToken(accessToken);

        return UserDto.UserInfo.builder()
                .userId(users.getUserId())
                .nickname(users.getNickname())
                .build();
    }


    //유저 정보 변경
    public void updateUserInfo(String accessToken, final UserDto.Update updateDto) {
        checkToken(accessToken);

        String role = jwtUtil.getRole(accessToken);
        if (role.equals(Role.ADMIN.getKey())) {
            Users targetUser = usersRepository.findById(Long.parseLong(updateDto.getId()))
                    .orElseThrow(() -> new UserNotFoundException("사용자 정보 불일치"));

            targetUser.updateInfo(updateDto);


        }
        throw new IllegalArgumentException("권한이 부족합니다.");
    }

    //유저 삭제
    public void deletedUser(String accessToken) {
        Users user = checkToken(accessToken);
        user.softDeleted();
    }

    //유저 목록
    @Transactional(readOnly = true)
    public List<UserDto.UserInfo> getAllUsers(String accessToken) {

        checkToken(accessToken);

        String userId = jwtUtil.getId(accessToken);

        return usersRepository.findAll().stream()
                .filter(u -> !userId.equals(u.getUserId()))
                .map(u -> new UserDto.UserInfo(u.getId().toString(), u.getTenantId(), u.getNickname(), u.getRole().toString()))
                .sorted(Comparator.comparingInt(u -> Integer.parseInt(u.getUserId())))
                .collect(Collectors.toList());
    }

    private Users checkToken(String accessToken) {
        Long id = Long.parseLong(jwtUtil.getId(accessToken));
        String tenantId = jwtUtil.getTenantId(accessToken);

        return usersRepository.findByIdAndTenantId(id, tenantId).orElseThrow(() ->
                new UserNotFoundException("사용자 정보 불일치"));
    }

    public UserDto.UserInfo login(UserDto.Login userDto) {

        log.info("userDto = {}", userDto.getUserId());
        Users userInfo = usersRepository.findByUserId(userDto.getUserId())
                .orElseThrow(() -> new UserNotFoundException("유저 정보가 없습니다."));

        boolean matches = encoder.matches(userDto.getPassword(), userInfo.getPassword());
        log.info("Password match result: {}", matches); // 이 로그 추가

        if (matches) {
            return UserDto.UserInfo.builder()
                    .userId(String.valueOf(userInfo.getId()))
                    .nickname(userInfo.getNickname())
                    .tenantId(userInfo.getTenantId())
                    .role(String.valueOf(userInfo.getRole()))
                    .build();
        }

        throw new IllegalArgumentException("아이디와 비밀번호를 확인해주세요.");
    }

    public void updatePassword(String accessToken, UserDto.Password userDto) {
        String userId = jwtUtil.getId(accessToken);

        Users userInfo = usersRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new UserNotFoundException("유저 정보가 없습니다."));

        if (!encoder.matches(userDto.getOrigin_password(), userInfo.getPassword())) {
            throw new IllegalArgumentException("기존 비밀번호가 일치하지 않습니다.");
        }

        if (encoder.matches(userDto.getPassword(), userInfo.getPassword())) {
            throw new IllegalArgumentException("기존 비밀번호와 동일한 비밀번호로 변경할 수 없습니다.");
        }

        if (!userDto.getPassword().equals(userDto.getConfirm_password())) {
            throw new IllegalArgumentException("새 비밀번호 확인이 일치하지 않습니다.");
        }

        userInfo.updatePassword(encoder.encode(userDto.getPassword()));
    }
}

