package com.backend.zero.DTO;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter @Setter
public class AuthRequest {
    private String email;
    private String username;
    private String password;
    private String role;
}
