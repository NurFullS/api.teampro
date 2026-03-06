package com.backend.zero.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServlet;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.backend.zero.DTO.ProjectDTO;
import com.backend.zero.model.Project;
import com.backend.zero.model.ProjectComment;
import com.backend.zero.model.ProjectTask;
import com.backend.zero.model.User;
import com.backend.zero.repository.ProjectCommentRepository;
import com.backend.zero.repository.ProjectRepository;
import com.backend.zero.repository.ProjectTaskRepository;
import com.backend.zero.repository.UserRepository;
import com.backend.zero.security.JwtUtil;

import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectTaskRepository taskRepository;
    private final ProjectCommentRepository commentRepository;
    private final JwtUtil jwtUtil;

    public ProjectController(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            ProjectTaskRepository taskRepository,
            ProjectCommentRepository commentRepository,
            JwtUtil jwtUtil) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.commentRepository = commentRepository;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/get-projects")
    public List<ProjectDTO> getMyProjects(@CookieValue("jwt") String token) {
        if (!jwtUtil.validateToken(token))
            throw new RuntimeException("Invalid token");

        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Project> projects = projectRepository.findAllByUserId(user.getId());

        List<ProjectDTO> dtos = new ArrayList<>();
        for (Project p : projects) {
            dtos.add(new ProjectDTO(
                    p.getId(),
                    p.getName(),
                    p.getDescription(),
                    p.getStatus(),
                    p.getOwner().getUsername(),
                    p.getOwner().getEmail(),
                    p.getAccessCode()));
        }
        return dtos;
    }

    @PostMapping
    public ProjectDTO createProject(@RequestBody ProjectDTO dto, @CookieValue("jwt") String token) {
        if (!jwtUtil.validateToken(token))
            throw new RuntimeException("Invalid token");

        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        Project project = new Project();
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setStatus(dto.getStatus());
        project.setOwner(user);
        project.setAccessCode(generateProjectCode());
        project.setDevelopers(new HashSet<>());

        Project saved = projectRepository.save(project);

        return new ProjectDTO(
                saved.getId(),
                saved.getName(),
                saved.getDescription(),
                saved.getStatus(),
                saved.getOwner().getUsername(),
                saved.getOwner().getEmail(),
                saved.getAccessCode());
    }

    private String generateProjectCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 15; i++) {
            int idx = (int) (Math.random() * chars.length());
            code.append(chars.charAt(idx));
        }
        return code.toString();
    }

    @DeleteMapping("/delete/{id}")
    public void deleteProject(@CookieValue("jwt") String token, @PathVariable Long id) {
        if (!jwtUtil.validateToken(token))
            throw new RuntimeException("Invalid token");

        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        Project project = projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getOwner().getId().equals(user.getId()))
            throw new RuntimeException("Unauthorized to delete this project");

        projectRepository.delete(project);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDTO> getProjectById(@CookieValue("jwt") String token, @PathVariable Long id) {
        if (!jwtUtil.validateToken(token))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Project project = projectRepository.findById(id).orElse(null);
        if (project == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        boolean isOwner = project.getOwner().getId().equals(user.getId());
        boolean isDeveloper = project.getDevelopers().contains(user);

        if (!isOwner && !isDeveloper)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        ProjectDTO dto = new ProjectDTO(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                project.getOwner().getUsername(),
                project.getOwner().getEmail(),
                project.getAccessCode());

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{projectId}/add-developer")
    public ResponseEntity<?> addDeveloperByEmail(
            @CookieValue("jwt") String token,
            @PathVariable Long projectId,
            @RequestParam String email) {

        if (!jwtUtil.validateToken(token))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");

        String ownerEmail = jwtUtil.extractEmail(token);
        User owner = userRepository.findByEmail(ownerEmail).orElse(null);
        if (owner == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Project not found");

        if (!project.getOwner().getId().equals(owner.getId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Вы не владелец проекта");

        User developer = userRepository.findByEmail(email).orElse(null);
        if (developer == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Пользователь с таким email не найден");

        if (project.getDevelopers().contains(developer))
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Разработчик уже добавлен");

        project.getDevelopers().add(developer);
        projectRepository.save(project);

        return ResponseEntity.ok("Разработчик успешно добавлен");
    }

    @PostMapping("/join")
    @Transactional
    public ResponseEntity<String> joinProject(@RequestParam String accessCode,
            @CookieValue("jwt") String token) {
        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Пользователь не найден");

        Project project = projectRepository.findByAccessCode(accessCode).orElse(null);
        if (project == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Проект не найден или неверный код");

        project.getDevelopers().add(user);
        projectRepository.save(project);


        return ResponseEntity.ok("Вы успешно присоединились к проекту");
    }

    @GetMapping("/users/all")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/{projectId}/developers")
    public ResponseEntity<Set<User>> getDevelopers(@CookieValue("jwt") String token, @PathVariable Long projectId) {
        if (!jwtUtil.validateToken(token))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        return ResponseEntity.ok(project.getDevelopers());
    }

    @DeleteMapping("/delete-developer/{projectId}/{developerId}")
    public ResponseEntity<String> deleteDeveloper(
            @CookieValue("jwt") String token,
            @PathVariable Long projectId,
            @PathVariable Long developerId) {

        if (!jwtUtil.validateToken(token))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");

        String ownerEmail = jwtUtil.extractEmail(token);
        User owner = userRepository.findByEmail(ownerEmail).orElse(null);
        if (owner == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Project not found");

        if (!project.getOwner().getId().equals(owner.getId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Вы не владелец проекта");

        User developer = userRepository.findById(developerId).orElse(null);
        if (developer == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Developer not found");

        if (!project.getDevelopers().contains(developer))
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Разработчик не состоит в проекте");

        project.getDevelopers().remove(developer);
        projectRepository.save(project);

        return ResponseEntity.ok("Разработчик успешно удален");
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<String> updateStatus(
            @CookieValue("jwt") String token,
            @PathVariable Long id,
            @RequestBody ProjectDTO dto) {

        if (!jwtUtil.validateToken(token))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");

        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");

        Project project = projectRepository.findById(id).orElse(null);
        if (project == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Project not found");

        if (!project.getOwner().getId().equals(user.getId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Вы не владелец проекта");

        project.setStatus(dto.getStatus());
        project.setName(dto.getName());
        projectRepository.save(project);

        return ResponseEntity.ok("Статус проекта обновлён");
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<ProjectTask> addTask(
            @CookieValue("jwt") String token,
            @PathVariable Long id,
            @RequestBody ProjectTask task) {

        if (!jwtUtil.validateToken(token))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        task.setProject(project);
        ProjectTask saved = taskRepository.save(task);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<ProjectTask>> getTasks(
            @CookieValue("jwt") String token,
            @PathVariable Long id) {

        if (!jwtUtil.validateToken(token))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<ProjectTask> tasks = taskRepository.findByProjectId(id);
        return ResponseEntity.ok(tasks);
    }

    @PutMapping("/{projectId}/tasks/{taskId}/toggle")
    public ResponseEntity<String> toggleTask(
            @CookieValue("jwt") String token,
            @PathVariable Long projectId,
            @PathVariable Long taskId) {

        if (!jwtUtil.validateToken(token))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");

        ProjectTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setDone(!task.isDone());
        taskRepository.save(task);

        return ResponseEntity.ok("Статус задачи изменён");
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(
            @CookieValue("jwt") String token,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        if (!jwtUtil.validateToken(token))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email).orElseThrow();

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        ProjectComment comment = new ProjectComment();
        comment.setText(body.get("text"));
        comment.setAuthor(user);
        comment.setProject(project);

        commentRepository.save(comment);
        return ResponseEntity.ok(comment);
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<ProjectComment>> getComments(@PathVariable Long id) {
        List<ProjectComment> comments = commentRepository.findAllByProjectId(id);
        return ResponseEntity.ok(comments);
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<String> deleteTask(
            @CookieValue("jwt") String token,
            @PathVariable Long taskId) {

        if (!jwtUtil.validateToken(token))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");

        ProjectTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        taskRepository.delete(task);

        return ResponseEntity.ok("Задача удалена");
    }

}
