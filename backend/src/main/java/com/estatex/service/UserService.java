package com.estatex.service;

import com.estatex.dto.LoginRequest;
import com.estatex.dto.LoginResponse;
import com.estatex.dto.RegisterRequest;
import com.estatex.dto.UpdateProfileRequest;
import com.estatex.dto.UserDto;
import com.estatex.dto.ChangePasswordRequest;
import com.estatex.model.Role;
import com.estatex.model.User;
import com.estatex.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service Layer implementing User Management and Authentication business logic.
 * Encapsulates role identification, password verification, forgot/reset password flows.
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // In-memory thread-safe store for Password Reset Tokens (valid for 30 minutes)
    private final Map<String, PasswordResetTokenInfo> resetTokens = new ConcurrentHashMap<>();

    @Autowired
    public UserService(UserRepository userRepository, EmailService emailService, JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Auto-migration hook: Ensures required database columns exist on startup.
     */
    @PostConstruct
    public void initDbColumns() {
        try {
            Integer colExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'profile_image'",
                    Integer.class
            );
            if (colExists == null || colExists == 0) {
                jdbcTemplate.execute("ALTER TABLE users ADD COLUMN profile_image LONGTEXT NULL");
                System.out.println("Auto-migration: Added 'profile_image' column to 'users' table.");
            }
        } catch (Exception e) {
            System.err.println("Database auto-migration notice: " + e.getMessage());
        }
    }

    /**
     * Authenticates a user given username (or email) and password.
     * Supports BCrypt (seed data & modern users) and legacy SHA-256 hashes.
     */
    public LoginResponse authenticate(LoginRequest request) {
        String identifier = request.getUsername().trim();
        String password = request.getPassword();

        // Query database by username or email
        Optional<User> userOpt = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier);

        if (userOpt.isEmpty()) {
            return LoginResponse.failure("No user account found matching username: " + identifier);
        }

        User user = userOpt.get();

        // Check password match (supports BCrypt & SHA-256)
        if (!verifyPassword(password, user.getPassword())) {
            return LoginResponse.failure("Invalid password. Please check your credentials.");
        }

        // Seamlessly upgrade legacy SHA-256 hash to BCrypt on successful login
        if (isLegacySha256Hash(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
        }

        // Identify role from user record and username format
        Role identifiedRole = user.getRole();
        if (identifiedRole == null) {
            identifiedRole = User.determineRoleFromUsername(user.getUsername());
            user.setRole(identifiedRole);
        }

        String roleName = identifiedRole.name();
        return new LoginResponse(
                true,
                "Authentication successful. Navigating to " + roleName + " portal.",
                user
        );
    }

    /**
     * Registers a new user account into the SQL database.
     * Formats username with '@user' role suffix and hashes password with BCrypt.
     */
    public UserDto register(RegisterRequest request) {
        String rawUsername = request.getUsername().trim();
        String password = request.getPassword();
        String email = request.getEmail().trim();

        // Strip any existing suffix and force @user to prevent privilege escalation
        String baseUsername = rawUsername;
        if (baseUsername.contains("@")) {
            baseUsername = baseUsername.substring(0, baseUsername.indexOf('@')).trim();
        }
        String formattedUsername = baseUsername + "@user";
        Role resolvedRole = Role.CUSTOMER;

        // Enforce unique username
        if (userRepository.existsByUsernameIgnoreCase(formattedUsername)) {
            throw new IllegalArgumentException("Username '" + formattedUsername + "' is already in use.");
        }

        // Enforce unique email
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email address '" + email + "' is already registered.");
        }

        User newUser = new User(
                formattedUsername,
                passwordEncoder.encode(password),
                email,
                request.getFullName().trim(),
                request.getPhone() != null ? request.getPhone().trim() : null,
                resolvedRole
        );

        User savedUser = userRepository.save(newUser);
        return new UserDto(savedUser);
    }

    /**
     * Change authenticated user password using their current password.
     */
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));

        // Verify current password
        if (!verifyPassword(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        if (request.getNewPassword() == null || request.getNewPassword().trim().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long.");
        }

        // Update with new BCrypt hashed password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * Initiates Forgot Password flow:
     * Generates a secure reset token, creates a reset link, and sends an email.
     */
    public Map<String, Object> initiateForgotPassword(String emailOrUsername, String baseUrl) {
        String identifier = emailOrUsername != null ? emailOrUsername.trim() : "";
        Optional<User> userOpt = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier);

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("No registered account found matching '" + identifier + "'. Please check and try again.");
        }

        User user = userOpt.get();
        String userEmail = user.getEmail();
        String username = user.getUsername();
        Long userId = user.getId();

        // Clean any expired tokens
        cleanExpiredTokens();

        // Generate an 8-character secure alphanumeric token
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(30);

        resetTokens.put(token, new PasswordResetTokenInfo(userId, username, userEmail, token, expiry));

        // Construct reset link
        String cleanBaseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.replaceAll("/+$", "") : "http://localhost:8080";
        String resetLink = cleanBaseUrl + "/user/reset-password.html?token=" + token;

        // Dispatch Email with username context
        boolean emailSent = emailService.sendPasswordResetEmail(userEmail, username, token, resetLink);

        return Map.of(
                "success", true,
                "message", "Password reset instructions have been sent to " + maskEmail(userEmail),
                "email", maskEmail(userEmail),
                "username", username,
                "token", token,
                "resetLink", resetLink,
                "emailDelivered", emailSent
        );
    }

    /**
     * Verifies if a reset token is valid and active.
     */
    public Map<String, Object> verifyResetToken(String token) {
        cleanExpiredTokens();
        PasswordResetTokenInfo info = resetTokens.get(token);
        if (info == null || info.isExpired()) {
            return Map.of("valid", false, "message", "Reset token is invalid or has expired. Please request a new one.");
        }
        return Map.of(
                "valid", true,
                "email", maskEmail(info.getEmail()),
                "username", info.getUsername() != null ? info.getUsername() : ""
        );
    }

    /**
     * Resets password using a validated reset token.
     */
    public String resetPasswordWithToken(String token, String newPassword) {
        cleanExpiredTokens();
        PasswordResetTokenInfo info = resetTokens.get(token);

        if (info == null || info.isExpired()) {
            throw new IllegalArgumentException("Invalid or expired password reset token. Please request a new one.");
        }

        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long.");
        }

        User user = null;
        if (info.getUserId() != null) {
            user = userRepository.findById(info.getUserId()).orElse(null);
        }
        if (user == null && info.getUsername() != null) {
            user = userRepository.findByUsernameIgnoreCase(info.getUsername()).orElse(null);
        }
        if (user == null && info.getEmail() != null) {
            user = userRepository.findByEmailIgnoreCase(info.getEmail()).orElse(null);
        }

        if (user == null) {
            throw new IllegalArgumentException("No account found associated with this reset token.");
        }

        String encodedPassword = passwordEncoder.encode(newPassword.trim());
        user.setPassword(encodedPassword);
        userRepository.saveAndFlush(user);
        userRepository.updatePasswordById(user.getId(), encodedPassword);

        // Invalidate used token
        resetTokens.remove(token);

        return user.getUsername();
    }

    /**
     * Verifies raw password against stored password (supports BCrypt, SHA-256, plaintext).
     */
    public boolean verifyPassword(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        if (isLegacySha256Hash(storedPassword)) {
            return storedPassword.equals(hashPasswordSha256(rawPassword));
        }
        return storedPassword.equals(rawPassword);
    }

    /**
     * Legacy SHA-256 hasher for checking old hashes.
     */
    private String hashPasswordSha256(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }

    private boolean isLegacySha256Hash(String hash) {
        return hash != null && hash.length() == 44 && hash.endsWith("=");
    }

    private void cleanExpiredTokens() {
        resetTokens.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String name = parts[0];
        if (name.length() <= 2) {
            return name.charAt(0) + "***@" + parts[1];
        }
        return name.substring(0, 2) + "***@" + parts[1];
    }

    /**
     * Resolves user by ID, username, or email.
     */
    public User findUserByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) return null;
        String clean = identifier.trim();

        // 1. Try parsing numeric ID
        try {
            Long id = Long.parseLong(clean);
            Optional<User> byId = userRepository.findById(id);
            if (byId.isPresent()) return byId.get();
        } catch (NumberFormatException ignored) {}

        // 2. Try username
        Optional<User> byUsername = userRepository.findByUsernameIgnoreCase(clean);
        if (byUsername.isPresent()) return byUsername.get();

        // 3. Try email
        return userRepository.findByEmailIgnoreCase(clean).orElse(null);
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> findByUsername(String username) {
        return Optional.ofNullable(findUserByIdentifier(username)).map(UserDto::new);
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> findByIdentifier(String identifier) {
        return Optional.ofNullable(findUserByIdentifier(identifier)).map(UserDto::new);
    }

    /**
     * Updates profile details (name, email, phone, address, profileImage) by identifier.
     */
    public UserDto updateProfile(String identifier, UpdateProfileRequest request) {
        User user = findUserByIdentifier(identifier);
        if (user == null) {
            throw new IllegalArgumentException("User not found with identifier: " + identifier);
        }

        if (!user.getEmail().equalsIgnoreCase(request.getEmail().trim()) && userRepository.existsByEmailIgnoreCase(request.getEmail().trim())) {
            throw new IllegalArgumentException("Email address '" + request.getEmail() + "' is already registered by another user.");
        }

        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail().trim());
        if (request.getPhone() != null) user.setPhone(request.getPhone().trim());
        if (request.getAddress() != null) user.setAddress(request.getAddress().trim());
        if (request.getProfileImage() != null && !request.getProfileImage().isBlank()) {
            user.setProfileImage(request.getProfileImage());
        }
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                Role newRole = Role.valueOf(request.getRole().trim().toUpperCase());
                user.setRole(newRole);
            } catch (Exception ignored) {}
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            user.setStatus(request.getStatus().trim());
        }
        if (request.getPassword() != null && request.getPassword().trim().length() >= 6) {
            user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        }

        User saved = userRepository.saveAndFlush(user);
        if (request.getProfileImage() != null && !request.getProfileImage().isBlank()) {
            try {
                userRepository.updateProfileImageById(saved.getId(), request.getProfileImage());
            } catch (Exception ignored) {}
        }
        UserDto dto = new UserDto(saved);
        if (saved.getRole() == Role.AGENT && saved.getId() != null) {
            try {
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM properties WHERE agent_id = ?",
                        Integer.class,
                        saved.getId()
                );
                dto.setPropertyCount(count != null ? count : 0);
            } catch (Exception ignored) {
                dto.setPropertyCount(0);
            }
        }
        return dto;
    }

    /**
     * Updates and persists profile image in database.
     */
    public UserDto updateProfilePhoto(String identifier, String profileImage) {
        User user = findUserByIdentifier(identifier);
        if (user == null) {
            throw new IllegalArgumentException("User not found with identifier: " + identifier);
        }

        user.setProfileImage(profileImage);
        User saved = userRepository.saveAndFlush(user);
        try {
            userRepository.updateProfileImageById(saved.getId(), profileImage);
        } catch (Exception ignored) {}

        return new UserDto(saved);
    }

    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsernameIgnoreCase(username.trim());
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return getAllUsers(null);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers(String roleFilter) {
        List<User> list;
        if (roleFilter != null && !roleFilter.isBlank()) {
            try {
                Role r = Role.valueOf(roleFilter.trim().toUpperCase());
                list = userRepository.findByRole(r);
            } catch (Exception e) {
                list = userRepository.findAll();
            }
        } else {
            list = userRepository.findAll();
        }

        return list.stream().map(u -> {
            UserDto dto = new UserDto(u);
            if (u.getRole() == Role.AGENT && u.getId() != null) {
                try {
                    Integer count = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM properties WHERE agent_id = ?",
                            Integer.class,
                            u.getId()
                    );
                    dto.setPropertyCount(count != null ? count : 0);
                } catch (Exception ignored) {
                    dto.setPropertyCount(0);
                }
            }
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Deletes user account and cascades dependent records (bookings, inquiries, reviews, properties).
     */
    public void deleteByIdentifier(String identifier) {
        User user = findUserByIdentifier(identifier);
        if (user == null) {
            throw new IllegalArgumentException("User not found with identifier: " + identifier);
        }

        Long userId = user.getId();

        // Safe cascade cleanup in case database tables lack CASCADE foreign key definitions
        try {
            jdbcTemplate.update("DELETE FROM reviews WHERE customer_id = ?", userId);
            jdbcTemplate.update("DELETE FROM inquiries WHERE customer_id = ?", userId);
            jdbcTemplate.update("DELETE FROM bookings WHERE customer_id = ?", userId);

            List<Long> propIds = jdbcTemplate.queryForList("SELECT id FROM properties WHERE agent_id = ?", Long.class, userId);
            for (Long propId : propIds) {
                jdbcTemplate.update("DELETE FROM reviews WHERE property_id = ?", propId);
                jdbcTemplate.update("DELETE FROM inquiries WHERE property_id = ?", propId);
                jdbcTemplate.update("DELETE FROM bookings WHERE property_id = ?", propId);
                jdbcTemplate.update("DELETE FROM property_images WHERE property_id = ?", propId);
            }
            jdbcTemplate.update("DELETE FROM properties WHERE agent_id = ?", userId);
        } catch (Exception ex) {
            System.err.println("Notice during cascade delete for user " + userId + ": " + ex.getMessage());
        }

        userRepository.delete(user);
        userRepository.flush();
    }

    public void deleteUser(Long id) {
        deleteByIdentifier(String.valueOf(id));
    }

    /**
     * Creates a new user (Admin User Management portal).
     */
    public UserDto createUser(Map<String, String> request) {
        String fullName = request.get("fullName");
        String email = request.get("email");
        String phone = request.get("phone");
        String password = request.get("password");
        String roleStr = request.get("role");
        String rawUsername = request.get("username");

        if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("Full name is required.");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required.");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password is required.");

        Role role = Role.CUSTOMER;
        if (roleStr != null) {
            String r = roleStr.trim().toUpperCase();
            if (r.contains("ADMIN")) role = Role.ADMIN;
            else if (r.contains("AGENT")) role = Role.AGENT;
            else role = Role.CUSTOMER;
        }

        String suffix = role == Role.ADMIN ? "@admin" : (role == Role.AGENT ? "@agent" : "@user");
        String baseUsername;
        if (rawUsername != null && !rawUsername.isBlank()) {
            baseUsername = rawUsername.trim();
            if (baseUsername.contains("@")) {
                baseUsername = baseUsername.substring(0, baseUsername.indexOf('@')).trim();
            }
        } else {
            baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
        }
        String formattedUsername = baseUsername + suffix;

        if (userRepository.existsByUsernameIgnoreCase(formattedUsername)) {
            throw new IllegalArgumentException("Username '" + formattedUsername + "' is already in use.");
        }
        if (userRepository.existsByEmailIgnoreCase(email.trim())) {
            throw new IllegalArgumentException("Email '" + email + "' is already registered.");
        }

        String address = request.get("address");
        if (address == null || address.isBlank()) {
            address = request.get("agency");
        }

        User newUser = new User(
                formattedUsername,
                passwordEncoder.encode(password.trim()),
                email.trim(),
                fullName.trim(),
                phone != null ? phone.trim() : null,
                role
        );
        if (address != null && !address.isBlank()) {
            newUser.setAddress(address.trim());
        }
        String status = request.get("status");
        if (status != null && !status.isBlank()) {
            newUser.setStatus(status.trim());
        }
        User saved = userRepository.saveAndFlush(newUser);
        UserDto dto = new UserDto(saved);
        dto.setPropertyCount(0);
        return dto;
    }

    /**
     * Token metadata helper class.
     */
    public static class PasswordResetTokenInfo {
        private final Long userId;
        private final String username;
        private final String email;
        private final String token;
        private final LocalDateTime expiryTime;

        public PasswordResetTokenInfo(Long userId, String username, String email, String token, LocalDateTime expiryTime) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.token = token;
            this.expiryTime = expiryTime;
        }

        public Long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }

        public String getToken() {
            return token;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }
    }
}
