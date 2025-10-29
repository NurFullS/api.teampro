package com.backend.zero.DTO;

import lombok.Data;

@Data
public class AuthResponse {
    private String email;
    private String username;
    private String role;
    private String token;

    public AuthResponse(String email, String username, String role, String token) {
        this.email = email;
        this.username = username;
        this.role = role;
        this.token = token;
    }
}
