package com.smartinventory.inventory.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "permissions")
public class PermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Permission name is mandatory")
    private String name;

    private String description;

    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    private Set<RoleEntity> roles = new HashSet<>();

    public PermissionEntity() {}

    public PermissionEntity(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<RoleEntity> getRoles() { return roles; }
    public void setRoles(Set<RoleEntity> roles) { this.roles = roles; }
}
