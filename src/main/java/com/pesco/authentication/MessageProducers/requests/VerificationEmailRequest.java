package com.pesco.authentication.MessageProducers.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class VerificationEmailRequest {

    private String email;
    private String content;
    private String link;
    private String username;

    // Default constructor
    public VerificationEmailRequest() {
    }

    // Constructor to map from JSON
    @JsonCreator
    public VerificationEmailRequest(
            @JsonProperty("email") String email,
            @JsonProperty("content") String content,
            @JsonProperty("link") String link,
            @JsonProperty("username") String username) {
        this.email = email;
        this.content = content;
        this.link = link;
        this.username = username;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
