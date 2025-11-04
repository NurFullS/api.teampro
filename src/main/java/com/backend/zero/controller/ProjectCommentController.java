package com.backend.zero.controller;

import com.backend.zero.model.Project;
import com.backend.zero.model.ProjectComment;
import com.backend.zero.model.User;
import com.backend.zero.repository.ProjectCommentRepository;
import com.backend.zero.repository.ProjectRepository;
import com.backend.zero.repository.UserRepository;
import com.backend.zero.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
public class ProjectCommentController {

    private final ProjectCommentRepository commentRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public ProjectCommentController(ProjectCommentRepository commentRepository,
                                    ProjectRepository projectRepository,
                                    UserRepository userRepository,
                                    JwtUtil jwtUtil) {
        this.commentRepository = commentRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ProjectComment>> getComments(@PathVariable Long projectId,
                                                            @CookieValue("jwt") String token) {
        if (!jwtUtil.validateToken(token))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        List<ProjectComment> comments = commentRepository.findAllByProjectOrderByCreatedAtAsc(project);
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/project/{projectId}")
    public ResponseEntity<String> addComment(@PathVariable Long projectId,
                                             @RequestBody String text,
                                             @CookieValue("jwt") String token) {
        if (!jwtUtil.validateToken(token))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");

        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Project not found");

        ProjectComment comment = new ProjectComment();
        comment.setProject(project);
        comment.setAuthor(user);
        comment.setText(text);
        commentRepository.save(comment);

        return ResponseEntity.ok("Комментарий добавлен");
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<String> deleteComment(@PathVariable Long commentId,
                                                @CookieValue("jwt") String token) {
        if (!jwtUtil.validateToken(token))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");

        ProjectComment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Comment not found");

        String email = jwtUtil.extractEmail(token);
        if (!comment.getAuthor().getEmail().equals(email))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not the author");

        commentRepository.delete(comment);
        return ResponseEntity.ok("Комментарий удален");
    }
}
