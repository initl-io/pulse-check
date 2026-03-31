package com.pulsecheck.web;

import com.pulsecheck.domain.user.AppUser;
import com.pulsecheck.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final UserService userService;

    @GetMapping
    public String settingsPage(Model model, Authentication auth) {
        AppUser user = userService.getUser(auth.getName());
        model.addAttribute("username", user.getUsername());
        model.addAttribute("updatedAt", user.getUpdatedAt());
        return "settings";
    }

    /**
     * 비밀번호 변경 API.
     * CSRF는 th:action 폼을 통해 자동 포함되므로 별도 처리 불필요.
     * 성공 시 세션 무효화 후 프론트에서 /logout으로 이동한다.
     */
    @PostMapping("/password")
    @ResponseBody
    public ResponseEntity<String> changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Authentication auth) {

        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }
        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest().body("비밀번호는 최소 8자 이상이어야 합니다.");
        }
        if (newPassword.equals(currentPassword)) {
            return ResponseEntity.badRequest().body("현재 비밀번호와 동일한 비밀번호로 변경할 수 없습니다.");
        }

        try {
            userService.changePassword(auth.getName(), currentPassword, newPassword);
            return ResponseEntity.ok("비밀번호가 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
