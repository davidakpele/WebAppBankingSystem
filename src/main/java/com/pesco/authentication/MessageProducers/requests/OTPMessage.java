package com.pesco.authentication.MessageProducers.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OTPMessage {
    private String email;
    private String otp;
    private String restPassword;
    private String configTwoFactorAuth;
    private String configTwoFactorAuthRecovery;

    // Default constructor
    public OTPMessage() {
    }

    // Constructor to map from JSON
    @JsonCreator
    public OTPMessage(
            @JsonProperty("email") String email,
            @JsonProperty("otp") String otp,
            @JsonProperty("restPassword") String restPassword,
            @JsonProperty("configTwoFactorAuth") String configTwoFactorAuth,
            @JsonProperty("configTwoFactorAuthRecovery") String configTwoFactorAuthRecovery) {
        this.email = email;
        this.otp = otp;
        this.restPassword = restPassword;
        this.configTwoFactorAuth = configTwoFactorAuth;
        this.configTwoFactorAuthRecovery = configTwoFactorAuthRecovery;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getRestPassword() {
        return restPassword;
    }

    public void setRestPassword(String restPassword) {
        this.restPassword = restPassword;
    }

    public String getConfigTwoFactorAuth() {
        return configTwoFactorAuth;
    }

    public void setConfigTwoFactorAuth(String configTwoFactorAuth) {
        this.configTwoFactorAuth = configTwoFactorAuth;
    }

    public String getConfigTwoFactorAuthRecovery() {
        return configTwoFactorAuthRecovery;
    }

    public void setConfigTwoFactorAuthRecovery(String configTwoFactorAuthRecovery) {
        this.configTwoFactorAuthRecovery = configTwoFactorAuthRecovery;
    }
}
