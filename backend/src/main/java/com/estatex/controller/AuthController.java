package com.estatex.controller;

import com.estatex.dto.ApiResponse;
import com.estatex.dto.ForgotPasswordRequest;
import com.estatex.dto.LoginRequest;
import com.estatex.dto.LoginResponse;
import com.estatex.dto.RegisterRequest;
import com.estatex.dto.ResetPasswordRequest;
import com.estatex.dto.UserDto;
import com.estatex.model.Role;
import com.estatex.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for User Authentication, Registration & Password Recovery.
 * Handles HTTP requests from Sign-In, Register, and Forgot/Reset Password pages.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(originPatterns = "*")
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Sign-in endpoint: Authenticates username/email & password against the database.
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.authenticate(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    /**
     * Create Account / Registration endpoint.
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            UserDto createdUser = userService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    new ApiResponse(true, "Account created successfully for " + createdUser.getUsername(), createdUser)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("Failed to complete registration: " + e.getMessage())
            );
        }
    }

    /**
     * Forgot Password endpoint: Generates token and sends password reset link to user email.
     * POST /api/auth/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        try {
            String origin = request.getBaseUrl();
            if (origin == null || origin.isBlank()) {
                origin = httpRequest.getHeader("Origin");
            }
            if (origin == null || origin.isBlank()) {
                String referer = httpRequest.getHeader("Referer");
                if (referer != null && !referer.isBlank()) {
                    try {
                        java.net.URI uri = new java.net.URI(referer);
                        int port = uri.getPort();
                        origin = uri.getScheme() + "://" + uri.getHost() + (port == -1 || port == 80 || port == 443 ? "" : ":" + port);
                    } catch (Exception ignored) {}
                }
            }
            if (origin == null || origin.isBlank() || origin.contains("8081")) {
                origin = "http://localhost:8080";
            }

            Map<String, Object> result = userService.initiateForgotPassword(request.getEmail(), origin);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("Failed to process forgot password request: " + e.getMessage())
            );
        }
    }

    /**
     * Verify Reset Token endpoint: Checks if a password reset token is valid and active.
     * GET /api/auth/verify-reset-token?token=xyz
     */
    @GetMapping("/verify-reset-token")
    public ResponseEntity<?> verifyResetToken(@RequestParam String token) {
        Map<String, Object> result = userService.verifyResetToken(token);
        return ResponseEntity.ok(result);
    }

    /**
     * Reset Password endpoint: Sets new password using a verified reset token.
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            String updatedUsername = userService.resetPasswordWithToken(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Your password has been successfully reset! You can now sign in with your new password.",
                    "username", updatedUsername != null ? updatedUsername : ""
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("Failed to reset password: " + e.getMessage())
            );
        }
    }

    /**
     * Check if a username is available.
     * GET /api/auth/check-username?username=dilshan
     */
    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsername(@RequestParam String username) {
        boolean available = userService.isUsernameAvailable(username);
        return ResponseEntity.ok(Map.of(
                "username", username,
                "available", available
        ));
    }

    /**
     * List user roles available in the system.
     * GET /api/auth/roles
     */
    @GetMapping("/roles")
    public ResponseEntity<List<Map<String, String>>> getRoles() {
        List<Map<String, String>> roles = Arrays.stream(Role.values())
                .map(r -> Map.of(
                        "name", r.name(),
                        "displayName", r.getDisplayName(),
                        "dashboardUrl", r.getDefaultDashboardUrl()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(roles);
    }
}
