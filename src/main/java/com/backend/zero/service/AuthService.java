package com.backend.zero.service;

import com.backend.zero.model.User;
import com.backend.zero.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String email, String password, String username, String role) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    public Optional<User> login(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(password, u.getPassword()));
    }
}
