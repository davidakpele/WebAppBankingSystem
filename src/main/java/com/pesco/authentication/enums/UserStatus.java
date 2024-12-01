package com.pesco.authentication.enums;

public enum UserStatus {
    ACTIVE("User is active"),
    BANNED("User is banned for suspicious activities or violations"),
    SUSPENDED("User is temporarily suspended"),
    PENDING_VERIFICATION("User account is pending verification");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
