package com.estatex.controller;

import com.estatex.dto.ApiResponse;
import com.estatex.dto.UpdateProfileRequest;
import com.estatex.dto.UserDto;
import com.estatex.dto.ChangePasswordRequest;
import com.estatex.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for User Management operations (viewing profile, updating profile, changing password, listing users, deleting users).
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(originPatterns = "*")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * List all users in system (used by Admin Dashboard User Management and Agent Management tables).
     * GET /api/users?role=AGENT
     */
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers(@RequestParam(required = false) String role) {
        return ResponseEntity.ok(userService.getAllUsers(role));
    }

    /**
     * Specific endpoint for agents.
     * GET /api/users/agents
     */
    @GetMapping("/agents")
    public ResponseEntity<List<UserDto>> getAgents() {
        return ResponseEntity.ok(userService.getAllUsers("AGENT"));
    }

    /**
     * Retrieve user profile by ID, username, or email.
     * GET /api/users/{identifier}
     */
    @GetMapping("/{identifier}")
    public ResponseEntity<?> getUserByIdentifier(@PathVariable String identifier) {
        return userService.findByIdentifier(identifier)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new user account (Admin Portal).
     * POST /api/users
     */
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> request) {
        try {
            UserDto createdUser = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "User created successfully.", createdUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Update user profile by ID, username, or email.
     * PUT /api/users/{identifier}
     */
    @PutMapping("/{identifier}")
    public ResponseEntity<?> updateProfile(@PathVariable String identifier, @Valid @RequestBody UpdateProfileRequest request) {
        try {
            UserDto updatedUser = userService.updateProfile(identifier, request);
            return ResponseEntity.ok(new ApiResponse(true, "Profile updated successfully.", updatedUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Dedicated endpoint to save user profile photo directly to database.
     * PUT /api/users/{identifier}/photo
     */
    @PutMapping("/{identifier}/photo")
    public ResponseEntity<?> updateProfilePhoto(@PathVariable String identifier, @RequestBody Map<String, String> payload) {
        try {
            String photoData = payload.get("profileImage");
            if (photoData == null || photoData.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Profile image data is required."));
            }
            UserDto updated = userService.updateProfilePhoto(identifier, photoData);
            return ResponseEntity.ok(new ApiResponse(true, "Profile photo saved to database successfully.", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete user account by ID, username, or email.
     * Cascades dependent records to ensure foreign key integrity.
     * DELETE /api/users/{identifier}
     */
    @DeleteMapping("/{identifier}")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable String identifier) {
        try {
            userService.deleteByIdentifier(identifier);
            return ResponseEntity.ok(ApiResponse.success("User account deleted successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete user: " + e.getMessage()));
        }
    }

    /**
     * Change user password.
     * PUT /api/users/{username}/password
     */
    @PutMapping("/{username}/password")
    public ResponseEntity<ApiResponse> changePassword(@PathVariable String username, @Valid @RequestBody ChangePasswordRequest request) {
        try {
            userService.changePassword(username, request);
            return ResponseEntity.ok(ApiResponse.success("Password updated successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
