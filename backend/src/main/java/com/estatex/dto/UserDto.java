package com.estatex.dto;

import com.estatex.model.Role;
import com.estatex.model.User;

/**
 * Safe public representation of a User entity (excludes password hash).
 */
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private Role role;
    private String roleDisplayName;
    private String profileImage;
    private Integer propertyCount = 0;
    private String agency;
    private String status = "Active";

    public UserDto() {
    }

    public UserDto(User user) {
        if (user != null) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.email = user.getEmail();
            this.fullName = user.getFullName();
            this.phone = user.getPhone();
            this.address = user.getAddress();
            this.agency = user.getAddress();
            this.role = user.getRole();
            this.roleDisplayName = user.getRole() != null ? user.getRole().getDisplayName() : "";
            this.profileImage = user.getProfileImage();
            this.status = (user.getStatus() != null && !user.getStatus().isBlank()) ? user.getStatus() : "Active";
        }
    }

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

    public String getRoleDisplayName() {
        return roleDisplayName;
    }

    public void setRoleDisplayName(String roleDisplayName) {
        this.roleDisplayName = roleDisplayName;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public Integer getPropertyCount() {
        return propertyCount;
    }

    public void setPropertyCount(Integer propertyCount) {
        this.propertyCount = propertyCount;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    public String getStatus() {
        return (status != null && !status.isBlank()) ? status : "Active";
    }

    public void setStatus(String status) {
        this.status = (status != null && !status.isBlank()) ? status : "Active";
    }
}
