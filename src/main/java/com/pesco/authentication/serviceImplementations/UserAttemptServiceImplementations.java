package com.pesco.authentication.serviceImplementations;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.pesco.authentication.enums.AttemptType;
import com.pesco.authentication.models.UserAttempt;
import com.pesco.authentication.models.UserRecord;
import com.pesco.authentication.repositories.UserAttemptRepository;
import com.pesco.authentication.repositories.UserRecordRepository;
import com.pesco.authentication.services.UserAttemptService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAttemptServiceImplementations implements UserAttemptService {

    private final UserAttemptRepository userAttemptRepository;
    private final UserRecordRepository userRecordRepository;

    @Override
    public ResponseEntity<?> createFailAttempt(Long id, AttemptType login) {

        UserAttempt attempt = userAttemptRepository.findByUserId(id);
        if (attempt == null) {
            
            // Create a new UserAttempt record if none exists
            UserAttempt createNew = UserAttempt.builder()
                .userId(id)
                .counter(1)
                .success(true)
                .attemptType(login)
                .timestamp(Timestamp.from(Instant.now()))
            .build();
            userAttemptRepository.save(createNew);
        } else {
            // Increment the badAttempts counter if a record already exists
            if (attempt.getCounter() >= 5) {
                attempt.setSuccess(true);
                userAttemptRepository.save(attempt);
                
                Optional<UserRecord> user = userRecordRepository.findByUserId(id);
                if (user.isPresent()) {
                    UserRecord updateStatus = user.get();
                    updateStatus.setLocked(true);

                    userRecordRepository.save(updateStatus);
                }
                //  Send Notification to user email
                return ResponseEntity.ok("This account has been locked due to too many attempts, Please contact our customer service.");
            } else if (attempt.getCounter() < 5) {
                attempt.setCounter(attempt.getCounter() + 1);
                userAttemptRepository.save(attempt);
            }
        }
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<?> UpdateUserAccount(Long id) {
        UserAttempt attempt = userAttemptRepository.findByUserId(id);
        if (attempt != null) {
            userAttemptRepository.deleteById(attempt.getId());
            Optional<UserRecord> user = userRecordRepository.findByUserId(id);
            if (user.isPresent()) {
                UserRecord updateStatus = user.get();
                updateStatus.setLocked(false);
                userRecordRepository.save(updateStatus);

                // Send Notification to user email

                return ResponseEntity.ok("Account has been successfully released.");
            }
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }
    
}
