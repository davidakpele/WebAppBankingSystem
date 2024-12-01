package com.pesco.authentication.serviceImplementations;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.pesco.authentication.models.PasswordResetToken;
import com.pesco.authentication.models.Users;
import com.pesco.authentication.payloads.ChangePasswordRequest;
import com.pesco.authentication.repositories.PasswordResetTokenRepository;
import com.pesco.authentication.repositories.UsersRepository;
import com.pesco.authentication.services.PasswordResetTokenService;
import com.pesco.authentication.responses.Error;

@Service
public class PasswordResetTokenServiceImplementation implements PasswordResetTokenService {
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsersRepository userRepository;

    public PasswordResetTokenServiceImplementation(PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder, UsersRepository userRepository) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @Override
    public ResponseEntity<?> findByToken(String token) {
        PasswordResetToken passOptional = passwordResetTokenRepository.findByToken(token);
        if (passOptional == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid token");
        }
        if (passOptional.isExpired()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Expired token");
        }
        return ResponseEntity.ok("valid");
    }

    @Override
    public boolean updatePassword(ChangePasswordRequest request) {
        PasswordResetToken passOptional = passwordResetTokenRepository.findByToken(request.getToken());

        if (passOptional != null && !passOptional.isExpired()) {
            Long userId = passOptional.getUserId();
            Optional<Users> userOptional = userRepository.findById(userId);

            if (userOptional.isPresent()) {
                Users user = userOptional.get();
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                userRepository.save(user);

                // Delete the token
                passwordResetTokenRepository.delete(passOptional);
                return true;
            }
        }

        return false;
    }

    @Override
    public ResponseEntity<?> resetPassword(ChangePasswordRequest request, Authentication authentication) {
        String username = authentication.getName();
        Optional<Users> optionUser = userRepository.findByUsername(username);

        if (optionUser != null && optionUser.isPresent()) {
            Users user = optionUser.get();
            if (passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                userRepository.save(user);

                return Error.createResponse("Password successfully updated.", HttpStatus.CREATED,
                        "Success");
            } else {
                return Error.createResponse("Currect Password do not match system password*", HttpStatus.BAD_REQUEST,
                        "The old password does not match with the your password in the system.");
            }

        }
        return Error.createResponse("Sorry, something went wrong.", HttpStatus.BAD_REQUEST,
                "Sorry, something went wrong in processing update.");
    }

    @Override
    public void createUserUserSession(Long userId, String token) {
        Optional<PasswordResetToken> passOptional = passwordResetTokenRepository.findByUserId(userId);
        // Delete existing token if present
        passOptional.ifPresent(passwordResetTokenRepository::delete);

        // Create and save new token
        PasswordResetToken newToken = new PasswordResetToken(token, userId);
        passwordResetTokenRepository.save(newToken);
    }

}
