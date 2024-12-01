package com.example.deposit.exceptions;

public class UserNotFoundException extends RuntimeException {
    private String details;

    public UserNotFoundException(String message, String details) {
        super(message);
        this.details = details;
    }

    public String getDetails() {
        return details;
    }
}
