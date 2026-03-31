package com.pulsecheck.domain.explorer;

import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 사용자가 명시적으로 허용한 로그 루트 경로.
 * 모든 파일 접근은 이 목록에 등록된 경로의 하위여야 한다.
 */
@Entity
@Table(name = "allowed_roots")
@Getter @Setter @NoArgsConstructor
public class AllowedRoot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 1024)
    private String path;

    @Column(nullable = false)
    private LocalDateTime registeredAt = LocalDateTime.now();

    public AllowedRoot(String path) {
        this.path = path;
    }
}
