package com.backend.zero.controller;

import com.backend.zero.model.User;
import com.backend.zero.repository.UserRepository;
import com.backend.zero.security.JwtUtil;
import com.backend.zero.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    private final UserRepository userRepository;

    public UserController(UserService userService, JwtUtil jwtUtil,
                          UserRepository userRepository) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public User getCurrentUser(HttpServletRequest request) {
        String token = null;
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (cookie.getName().equals("jwt")) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token == null || token.isEmpty()) {
            throw new RuntimeException("Unauthorized");
        }

        String email = jwtUtil.extractEmail(token);
        return userService.getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PutMapping("/{id}/update")
    public User updateProfile(
            @PathVariable Long id,
            @RequestBody User updatedUser,
            HttpServletRequest request) {

        String token = null;

        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (cookie.getName().equals("jwt")) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token == null) {
            throw new RuntimeException("Unauthorized");
        }

        String email = jwtUtil.extractEmail(token);

        User user = userService.getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setUsername(updatedUser.getUsername());
        user.setEmail(updatedUser.getEmail());
        user.setRole(updatedUser.getRole());
        user.setUserStatus(updatedUser.getUserStatus());

        return userService.save(user);
    }

    @GetMapping("/by-email/{email}")
    public Optional<User> getUserEmail(@PathVariable String email) {

        return userRepository.findByEmail(email);
    }

    @PutMapping("/update-status")
    public User updateWorkerStatus(HttpServletRequest request, @RequestParam String status) {
        String token = null;
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (cookie.getName().equals("jwt")) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token == null) {
            throw new RuntimeException("Unauthorized");
        }

        String email = jwtUtil.extractEmail(token);
        User user = userService.getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setUserStatus(status);
        return userService.save(user);
    }

    @GetMapping("/all-users")
    public List<User> allUsers(HttpServletRequest request) {
        return userService.getAllUsers();
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    userRepository.delete(user);
                    return ResponseEntity.ok("Юзер успешно удален!");
                })
                .orElseGet(() -> ResponseEntity.status(404).body("Такого юзера нету!"));
    }
}
