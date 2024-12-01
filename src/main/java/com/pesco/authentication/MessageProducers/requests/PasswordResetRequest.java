package com.pesco.authentication.MessageProducers.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PasswordResetRequest {
    private String email;
    private String username;
    private String content;
    private String url;
    private String customerEmail;

    // Default constructor
    public PasswordResetRequest() {
    }

    // Constructor to map from JSON
    @JsonCreator
    public PasswordResetRequest(
            @JsonProperty("email") String email,
            @JsonProperty("username") String username,
            @JsonProperty("content") String content,
            @JsonProperty("url") String url,
            @JsonProperty("customerEmail") String customerEmail) {
        this.email = email;
        this.username = username;
        this.content = content;
        this.url = url;
        this.customerEmail = customerEmail;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
}
