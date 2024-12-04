package com.dayone.service;

import com.dayone.exception.impl.AlreadyExistUserException;
import com.dayone.exception.impl.PasswordNotMatchException;
import com.dayone.exception.impl.UserNotFoundException;
import com.dayone.model.Auth;
import com.dayone.persist.MemberRepository;
import com.dayone.persist.entity.MemberEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.NotYetImplementedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class MemberService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        return this.memberRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException());
    }

    public MemberEntity register(Auth.SignUp member) {
        // 아이디가 존재하는 경우 exception 발생
        boolean exists = memberRepository.existsByUsername(member.getUsername());
        if (exists) {
            throw new AlreadyExistUserException();
        }
        // ID 생성 가능한 경우, 멤버 테이블에 저장
        MemberEntity memberEntity = MemberEntity.builder()
                .username(member.getUsername())
                // 비밀번호는 암호화 되어서 저장되어야함
                .password(passwordEncoder.encode(member.getPassword()))
                .roles(member.getRoles())
                .build();
        return memberRepository.save(memberEntity);
    }

    public MemberEntity authenticate(Auth.SignIn member) {
        // id 로 멤버 조회
        MemberEntity memberEntity = memberRepository.findByUsername(member.getUsername()).orElseThrow(
                () -> new UserNotFoundException()
        );

        // 패스워드 일치 여부 확인
        if (!passwordEncoder.matches(member.getPassword(), memberEntity.getPassword())) {
            //      - 일치하지 않는 경우 400 status 코드와 적합한 에러 메시지 반환
            throw new PasswordNotMatchException();
        }
        //      - 일치하는 경우, 해당 멤버 엔티티 반환
        return memberEntity;
    }
}
