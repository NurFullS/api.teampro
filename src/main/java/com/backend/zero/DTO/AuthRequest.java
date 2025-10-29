package com.backend.zero.DTO;

import lombok.Data;

@Data
public class AuthRequest {
    private String email;
    private String username;
    private String password;
    private String role;
}
