package com.pesco.authentication.services;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import com.pesco.authentication.payloads.ChangePasswordRequest;

public interface PasswordResetTokenService {
    void createUserUserSession(Long userId, String token);

    ResponseEntity<?> findByToken(String token);

    boolean updatePassword(ChangePasswordRequest request);

    ResponseEntity<?> resetPassword(ChangePasswordRequest request, Authentication authentication);
}
