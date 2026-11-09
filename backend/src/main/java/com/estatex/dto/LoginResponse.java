package com.estatex.dto;

import com.estatex.model.Role;
import com.estatex.model.User;

/**
 * Data Transfer Object returned upon authentication.
 * Includes user profile summary and role-based redirect URL.
 */
public class LoginResponse {
    private boolean success;
    private String message;
    private UserDto user;
    private Role role;
    private String redirectUrl;

    public LoginResponse() {
    }

    public LoginResponse(boolean success, String message, User user) {
        this.success = success;
        this.message = message;
        if (user != null) {
            this.user = new UserDto(user);
            this.role = user.getRole();
            this.redirectUrl = user.getDashboardRedirectUrl();
        }
    }

    public static LoginResponse failure(String message) {
        LoginResponse resp = new LoginResponse();
        resp.setSuccess(false);
        resp.setMessage(message);
        return resp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }
}
