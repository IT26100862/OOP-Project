package com.estatex.repository;

import com.estatex.model.Role;
import com.estatex.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object / Repository interface for User entities.
 * Provides SQL query methods to locate users by username, email, or role.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username (case-insensitive)
     */
    Optional<User> findByUsernameIgnoreCase(String username);

    /**
     * Find user by email (case-insensitive)
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Find user by either username or email
     */
    Optional<User> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

    /**
     * Check if a username is already registered
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Check if an email address is already registered
     */
    boolean existsByEmailIgnoreCase(String email);



    /**
     * List all users filtered by specific role
     */
    List<User> findByRole(Role role);

    /**
     * Explicit direct database update for password
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("UPDATE User u SET u.password = :password WHERE u.id = :id")
    int updatePasswordById(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("password") String password);

    /**
     * Explicit direct database update for profile image
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("UPDATE User u SET u.profileImage = :profileImage WHERE u.id = :id")
    int updateProfileImageById(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("profileImage") String profileImage);
}
