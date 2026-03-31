package com.pulsecheck.web;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;

/**
 * Spring Boot 기본 Whitelabel Error Page를 대체하는 커스텀 에러 컨트롤러.
 * HTTP 상태 코드에 따라 픽셀 레트로 스타일 에러 페이지로 라우팅한다.
 *
 * <p>Spring Boot가 {@code /error}로 포워드할 때 이 컨트롤러가 처리한다.
 */
@Controller
@RequestMapping("/error")
public class AppErrorController implements ErrorController {

    @RequestMapping
    public String handleError(HttpServletRequest request, Model model) {
        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object pathAttr   = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Object msgAttr    = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        int status = statusAttr != null ? Integer.parseInt(statusAttr.toString()) : 500;
        String path    = pathAttr != null ? pathAttr.toString() : request.getRequestURI();
        String message = msgAttr  != null ? msgAttr.toString()  : "";

        model.addAttribute("status",    status);
        model.addAttribute("error",     HttpStatus.resolve(status) != null
                ? HttpStatus.resolve(status).getReasonPhrase() : "Unknown Error");
        model.addAttribute("message",   message);
        model.addAttribute("path",      path);
        model.addAttribute("timestamp", LocalDateTime.now());

        // 상태 코드별 전용 템플릿 우선 시도, 없으면 계열 폴백
        switch (status) {
            case 400: return "error/4xx";
            case 401: return "error/403";   // Spring Security는 401 대신 403을 주로 반환
            case 403: return "error/403";
            case 404: return "error/404";
            case 429: return "error/4xx";
            case 500: return "error/500";
            default:  return status >= 500 ? "error/5xx" : "error/4xx";
        }
    }
}
