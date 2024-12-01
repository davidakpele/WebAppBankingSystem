package com.pesco.authentication.services;

import java.util.Optional;
import org.springframework.http.ResponseEntity;
import com.pesco.authentication.models.Users;
import com.pesco.authentication.payloads.UserSignInRequest;
import com.pesco.authentication.payloads.UserSignUpRequest;
import com.pesco.authentication.responses.VerificationTokenResult;
import jakarta.servlet.http.HttpServletResponse;
public interface AuthenticationService {

    ResponseEntity<?> createAccount(UserSignUpRequest request);

    ResponseEntity<?> login(UserSignInRequest request, HttpServletResponse response);

    Optional<Users> findByEmail(String email);

    Optional<Users> findByUsername(String username);

    ResponseEntity<?> verifyUser(String token, Long id);

    VerificationTokenResult generateVerificationToken(String oldToken);
}
