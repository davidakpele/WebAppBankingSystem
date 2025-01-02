package com.pesco.authentication.services;

import org.springframework.http.ResponseEntity;
import com.pesco.authentication.enums.AttemptType;

public interface UserAttemptService {

	ResponseEntity<?> createFailAttempt(Long id, AttemptType login);

	ResponseEntity<?> UpdateUserAccount(Long id);

}
