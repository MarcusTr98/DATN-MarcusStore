package com.fpoly.marcusstore.entity.auth;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Employee_Positions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeePosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "position_id")
    private Integer positionId;

    @Column(name = "position_name", nullable = false, unique = true)
    private String positionName;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active")
    private Boolean isActive;

@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;

@PrePersist
public void prePersist() {

    if (createdAt == null) {
        createdAt = LocalDateTime.now();
    }

    if (isActive == null) {
        isActive = true;
    }

};

@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
        name = "Employee_Position_Permissions",
        joinColumns = @JoinColumn(name = "position_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
)
private Set<Permission> permissions = new HashSet<>();

}