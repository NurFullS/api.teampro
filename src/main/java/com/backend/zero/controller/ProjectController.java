package com.backend.zero.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.zero.model.Project;
import com.backend.zero.model.User;
import com.backend.zero.repository.ProjectRepository;
import com.backend.zero.repository.UserRepository;
import com.backend.zero.security.JwtUtil;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public ProjectController(ProjectRepository projectRepository, UserRepository userRepository, JwtUtil jwtUtil) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    // Получаем проекты текущего пользователя
    @GetMapping("/get-projects")
    public List<Project> getMyProjects(@CookieValue("jwt") String token) {
        if (!jwtUtil.validateToken(token))
            throw new RuntimeException("Invalid token");
        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return projectRepository.findAllByOwnerId(user.getId());
    }

    // Создаем проект и привязываем к текущему пользователю
    @PostMapping
    public Project createProject(@RequestBody Project project, @CookieValue("jwt") String token) {
        // Проверяем токен
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Invalid token");
        }

        // Извлекаем email пользователя из токена
        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Привязываем проект к пользователю
        project.setOwner(user);

        // Сохраняем проект в базе
        return projectRepository.save(project);
    }

}
