package com.backend.zero.controller;

import com.backend.zero.DTO.AuthRequest;
import com.backend.zero.DTO.AuthResponse;
import com.backend.zero.model.User;
import com.backend.zero.security.JwtUtil;
import com.backend.zero.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody AuthRequest request) {
        User user = authService.register(
                request.getEmail(),
                request.getPassword(),
                request.getUsername(),
                request.getRole());
        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(user.getEmail(), user.getUsername(), user.getRole(), token);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request, HttpServletResponse response) {
        return authService.login(request.getEmail(), request.getPassword())
                .map(user -> {
                    // Генерируем токен
                    String token = jwtUtil.generateToken(user.getEmail());

                    // Ставим cookie
                    Cookie cookie = new Cookie("jwt", token);
                    cookie.setHttpOnly(true);
                    cookie.setPath("/");
                    response.addCookie(cookie);

                    AuthResponse authResponse = new AuthResponse(
                            user.getEmail(),
                            user.getUsername(),
                            user.getRole(),
                            token);

                    return ResponseEntity.ok(authResponse);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new AuthResponse(null, null, null, "Неверный email или пароль")));
    }

}
