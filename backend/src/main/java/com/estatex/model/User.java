package com.estatex.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Domain Entity representing a registered user in EstateX.
 * Encapsulates user credentials, personal profile, and role-based permissions.
 * Follows core Object-Oriented principles (Encapsulation, Data Hiding).
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String password;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Lob
    @Column(name = "profile_image", columnDefinition = "LONGTEXT")
    private String profileImage;

    @Column(length = 20)
    private String status = "Active";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Default Constructor (required by JPA)
    public User() {
        this.createdAt = LocalDateTime.now();
    }

    // Parameterized Constructor
    public User(String username, String password, String email, String fullName, String phone, Role role) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.role = role != null ? role : determineRoleFromUsername(username);
        this.createdAt = LocalDateTime.now();
    }

    // Pre-persist hook to populate createdAt timestamp
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.role == null && this.username != null) {
            this.role = determineRoleFromUsername(this.username);
        }
    }

    /**
     * Determines user role based on username suffix:
     * - name@admin -> ADMIN
     * - name@agent -> AGENT
     * - name@user -> CUSTOMER
     */
    public static Role determineRoleFromUsername(String username) {
        if (username != null) {
            String lower = username.trim().toLowerCase();
            if (lower.endsWith("@admin")) {
                return Role.ADMIN;
            } else if (lower.endsWith("@agent")) {
                return Role.AGENT;
            } else if (lower.endsWith("@user")) {
                return Role.CUSTOMER;
            }
        }
        return Role.CUSTOMER;
    }

    /**
     * Determines the target dashboard page based on the user's role.
     * @return relative URL string to the corresponding portal dashboard
     */
    public String getDashboardRedirectUrl() {
        Role effectiveRole = this.role != null ? this.role : determineRoleFromUsername(this.username);
        if (effectiveRole != null) {
            return effectiveRole.getDefaultDashboardUrl();
        }
        return "../user/dashboard.html";
    }

    // --- Getters and Setters (Encapsulation) ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getStatus() {
        return (status != null && !status.isBlank()) ? status : "Active";
    }

    public void setStatus(String status) {
        this.status = (status != null && !status.isBlank()) ? status : "Active";
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role=" + role +
                '}';
    }
}
