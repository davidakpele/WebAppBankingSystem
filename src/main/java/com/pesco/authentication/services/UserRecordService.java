package com.pesco.authentication.services;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import com.pesco.authentication.dto.UserDTO;
import com.pesco.authentication.models.UserRecord;


public interface UserRecordService {

    UserDTO getUserDetailsById(Long id, Authentication authentication);
    
    UserDTO getUserByUsername(String username, Authentication authentication);

    UserDTO findPublicUserByUsername(String username);

    Optional<UserRecord> getUserReferralCode(Long id);

    Optional<UserRecord> getUserNames(Long userId);

    boolean isLockedAccount(Long userId);

    boolean isBlockedAccount(Long userId);

    ResponseEntity<?> updateUserRecordTransferPinStatus(Long id, Authentication authentication);

    UserDTO findPublicUserByUserId(Long userId);

    ResponseEntity<?> lockUserAccount(Long userId);

    ResponseEntity<?> blockUserAccount(Long userId);
}