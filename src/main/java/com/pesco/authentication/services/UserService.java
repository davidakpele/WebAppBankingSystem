package com.pesco.authentication.services;

import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import com.pesco.authentication.models.Users;

public interface UserService {

    Users getUserByUsername(String username);

    Users getUserById(Long userId);

    Optional<Users> findById(Long id);

    ResponseEntity<?> resetPassword(Long userId, String password, Authentication authentication);

    Object deactivateAccount(Long id, Authentication authentication);

    String forgetPassword(String email);

    ResponseEntity<?> updateUserProfile(String username, String email, String gender, String profilePath);

}
