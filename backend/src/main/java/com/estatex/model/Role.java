package com.estatex.model;

/**
 * System User Roles for EstateX Property Management System.
 * Distinguishes between Standard User (Customer), Agent, and Administrator.
 */
public enum Role {
    CUSTOMER("Customer / Standard User", "../user/dashboard.html"),
    AGENT("Real Estate Agent", "../agent/dashboard.html"),
    ADMIN("System Administrator", "../admin/dashboard.html");

    private final String displayName;
    private final String defaultDashboardUrl;

    Role(String displayName, String defaultDashboardUrl) {
        this.displayName = displayName;
        this.defaultDashboardUrl = defaultDashboardUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultDashboardUrl() {
        return defaultDashboardUrl;
    }
}
