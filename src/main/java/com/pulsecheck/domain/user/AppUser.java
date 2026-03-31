package com.pulsecheck.domain.user;

import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_users")
@Getter @Setter @NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String username;

    /** BCrypt 해시 저장 */
    @Column(nullable = false, length = 256)
    private String password;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public AppUser(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
