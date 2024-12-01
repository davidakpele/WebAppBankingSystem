package com.pesco.authentication.serviceImplementations;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import com.pesco.authentication.dto.UserDTO;
import com.pesco.authentication.models.UserRecord;
import com.pesco.authentication.models.Users;
import com.pesco.authentication.repositories.UserRecordRepository;
import com.pesco.authentication.repositories.UsersRepository;
import com.pesco.authentication.services.UserRecordService;

@Service
public class UserRecordImplementations implements UserRecordService {

    private final UsersRepository userRepository;
    private final UserRecordRepository userRecordRepository;

    public UserRecordImplementations(UsersRepository userRepository, UserRecordRepository userRecordRepository) {
        this.userRepository = userRepository;
        this.userRecordRepository = userRecordRepository;
    }

    @Override
    public UserDTO getUserDetailsById(Long id, Authentication authentication) {
        Users user = userRepository.findUserWithRecordById(id);
        // Get the username from authentication
        String username = authentication.getName();
        if (user == null || !user.getUsername().equals(username)) {
            return null;
        }
        return UserDTO.fromEntity(user);
    }

    @Override
    public UserDTO getUserByUsername(String username, Authentication authentication) {
        Optional<Users> GetUser = userRepository.findByUsername(username);
        // Get the username from authentication
        String NewUsername = authentication.getName();
        if (GetUser.isPresent() && NewUsername.equals(GetUser.get().getUsername())) {
            Users user = userRepository.findUserWithRecordById(GetUser.get().getId());
            return UserDTO.fromEntity(user);
        }

        return null;
    }

    @Override
    public Optional<UserRecord> getUserReferralCode(Long id) {
        var userRecord = userRecordRepository.findByUserId(id);
        return userRecord;
    }

    @Override
    public boolean isLockedAccount(Long userId) {
        return userRecordRepository.isUserAccountLocked(userId);
    }

    @Override
    public boolean isBlockedAccount(Long userId) {
        return userRecordRepository.isUserAccountBlocked(userId);
    }

    @Override
    public Optional<UserRecord> getUserNames(Long userId) {
        return userRecordRepository.findByUserId(userId);
    }

    @Override
    public ResponseEntity<?> updateUserRecordTransferPinStatus(Long id, Authentication authentication) {
        Optional<Users> GetUser = userRepository.findById(id);
        // Get the username from authentication
        String NewUsername = authentication.getName();
        if (GetUser.isPresent() && NewUsername.equals(GetUser.get().getUsername())) {
            Optional<UserRecord> userRecord = userRecordRepository.findByUserId(id);
            if (userRecord.isPresent()) {
               UserRecord updateUserRecord = userRecord.get();
               updateUserRecord.set_transfer_pin(true);
               userRecordRepository.save(updateUserRecord);
            }
            return ResponseEntity.ok("Transfer Pin set successfully.");
        }

        return null;
    }

    @Override
    public UserDTO findPublicUserByUsername(String username) {
        Optional<Users> GetUser = userRepository.findByUsername(username);
        if (GetUser.isPresent()) {
            Users user = userRepository.findUserWithRecordById(GetUser.get().getId());
            return UserDTO.fromEntity(user);
        }

        return null;
    }

    @Override
    public UserDTO findPublicUserByUserId(Long userId) {
        Optional<Users> GetUser = userRepository.findById(userId);
        if (GetUser.isPresent()) {
            Users user = userRepository.findUserWithRecordById(GetUser.get().getId());
            return UserDTO.fromEntity(user);
        }

        return null;
    }

    

}
