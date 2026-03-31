package com.pulsecheck.domain.user;

import com.pulsecheck.config.AppProperties;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final AppUserRepository userRepo;
    private final PasswordEncoder   passwordEncoder;
    private final AppProperties     props;

    /**
     * 앱 최초 실행 시 기본 계정(admin / a123456*)을 생성한다.
     * 이미 계정이 존재하면 변경하지 않는다.
     */
    @PostConstruct
    void initDefaultUser() {
        if (userRepo.count() == 0) {
            AppProperties.Security sec = props.getSecurity();
            AppUser admin = new AppUser(
                    sec.getUsername(),
                    passwordEncoder.encode(sec.getPassword())
            );
            userRepo.save(admin);
            log.info("Default user created: {}", sec.getUsername());
        }
    }

    // ── Spring Security UserDetailsService ───────────────────

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())   // 이미 BCrypt 해시
                .roles("USER")
                .build();
    }

    // ── 비밀번호 변경 ─────────────────────────────────────────

    @Transactional
    public void changePassword(String username, String currentRaw, String newRaw) {
        AppUser user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다."));

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(currentRaw, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }

        user.setPassword(passwordEncoder.encode(newRaw));
        user.setUpdatedAt(LocalDateTime.now());
        log.info("Password changed for user: {}", username);
    }

    public AppUser getUser(String username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다."));
    }
}
