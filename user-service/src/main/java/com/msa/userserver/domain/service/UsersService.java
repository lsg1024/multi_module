package com.msa.userserver.domain.service;


import com.msa.userserver.domain.respository.UsersRepository;
import com.msa.userserver.domain.dto.UserDto;
import com.msa.userserver.domain.entity.Role;
import com.msa.userserver.domain.entity.Users;
import com.msa.userserver.exception.UserNotFoundException;
import com.msacommon.global.jwt.JwtUtil;
import com.msacommon.global.tenant.TenantContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
                .userId(Long.valueOf(userDto.getUserId()))
                .tenantId(tenantId)
                .password(encoder.encode(userDto.getPassword()))
                .nickname(userDto.getNickname())
                .role(Role.USER)
                .build();

        usersRepository.save(user);
    }

    //유저 정보 변경
    public void updateUserInfo(String accessToken, final UserDto.Update updateDto) {
        checkToken(accessToken);

        Users targetUser = usersRepository.findById(Long.parseLong(updateDto.getId())).orElseThrow(() ->
                new UserNotFoundException("사용자 정보 불일치"));

        targetUser.updateInfo(updateDto);
    }

    //유저 정보 확인 -> jwt 아이디 이용

    //유저 삭제
    public void deletedUser(String accessToken) {
        Users user = checkToken(accessToken);
        user.softDeleted();
    }

    //유저 목록
    public List<UserDto.UserInfo> getAllUsers(String accessToken) {

        checkToken(accessToken);

        return usersRepository.findAll().stream()
                .map(u -> new UserDto.UserInfo(u.getId().toString(), u.getTenantId(), u.getNickname(), u.getRole().toString()))
                .collect(Collectors.toList());

    }

    private Users checkToken(String accessToken) {
        Long id = Long.parseLong(jwtUtil.getId(accessToken));
        String tenantId = jwtUtil.getTenantId(accessToken);

        return usersRepository.findByIdAndTenantId(id, tenantId).orElseThrow(() ->
                new UserNotFoundException("사용자 정보 불일치"));
    }

    public UserDto.UserInfo login(UserDto.Login userDto) {
        Users userInfo = usersRepository.findByUserId(userDto.getUserId())
                .orElseThrow(() -> new RuntimeException("유저 정보가 없습니다."));

        boolean matches = encoder.matches(userDto.getPassword(), userInfo.getPassword());

        if (matches) {
            return UserDto.UserInfo.builder()
                    .id(String.valueOf(userInfo.getId()))
                    .nickname(userInfo.getNickname())
                    .tenantId(userInfo.getTenantId())
                    .role(String.valueOf(userInfo.getRole()))
                    .build();
        }

        throw new RuntimeException("유저 정보가 없습니다");
    }
}
