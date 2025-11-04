package com.backend.zero.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectDTO {
    private Long id;
    private String name;
    private String description;
    private String status;
    private String ownerUsername;
    private String ownerEmail;
    private String accessCode;
}
