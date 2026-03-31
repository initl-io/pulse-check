package com.pulsecheck.web;

import com.pulsecheck.domain.explorer.FileExplorerService;
import com.pulsecheck.domain.explorer.FileNode;
import com.pulsecheck.security.PathSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ExplorerController {

    private final FileExplorerService fileExplorerService;
    private final PathSecurityService pathSecurityService;

    @GetMapping("/")
    public String index(Model model, Authentication auth) {
        model.addAttribute("username", auth != null ? auth.getName() : "");
        return "index";
    }

    /** GET /login — Spring Security는 POST만 자동 처리하므로 GET은 반드시 직접 서빙해야 한다 */
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/api/files")
    @ResponseBody
    public ResponseEntity<?> getFileTree(@RequestParam(required = false) String path) throws Exception {
        FileNode tree = fileExplorerService.getFileTree(path);
        return ResponseEntity.ok(tree);
    }

    /** 등록된 허용 루트 목록 조회 */
    @GetMapping("/api/roots")
    @ResponseBody
    public ResponseEntity<?> getRoots() {
        return ResponseEntity.ok(pathSecurityService.getRegisteredRoots());
    }

    /** 허용 루트 삭제 */
    @DeleteMapping("/api/roots/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteRoot(@PathVariable Long id) {
        pathSecurityService.removeRoot(id);
        return ResponseEntity.ok(Map.of("deleted", id));
    }
}
