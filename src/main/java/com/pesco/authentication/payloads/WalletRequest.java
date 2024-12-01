package com.pesco.authentication.payloads;

public class WalletRequest {
    private Long userId;

    // Constructor
    public WalletRequest(Long userId) {
        this.userId = userId;
    }

    // Getters and setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
