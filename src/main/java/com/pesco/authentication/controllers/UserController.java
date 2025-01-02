package com.pesco.authentication.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import com.pesco.authentication.configurations.FileStorageConfig;
import com.pesco.authentication.dto.UserDTO;
import com.pesco.authentication.models.Users;
import com.pesco.authentication.payloads.UserSignUpRequest;
import com.pesco.authentication.payloads.ChangePasswordRequest;
import com.pesco.authentication.properties.KeysWrapper;
import com.pesco.authentication.responses.Error;
import com.pesco.authentication.services.PasswordResetTokenService;
import com.pesco.authentication.services.TwoFactorAuthenticationService;
import com.pesco.authentication.services.UserRecordService;
import com.pesco.authentication.services.UserService;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController 
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userServices;
    private final UserRecordService userRecordService;
    private final TwoFactorAuthenticationService twoFactorAuthenticationService;
    private final FileStorageConfig fileStorageConfig;
    private final KeysWrapper keysWrapper;
    private final PasswordResetTokenService passwordResetTokenService;

    public UserController(UserService userServices,
            UserRecordService userRecordService,
            TwoFactorAuthenticationService twoFactorAuthenticationService,
            FileStorageConfig fileStorageConfig,
            KeysWrapper keysWrapper,
            PasswordResetTokenService passwordResetTokenService) {
        this.userServices = userServices;
        this.userRecordService = userRecordService;
        this.twoFactorAuthenticationService = twoFactorAuthenticationService;
        this.fileStorageConfig = fileStorageConfig;
        this.keysWrapper = keysWrapper;
        this.passwordResetTokenService = passwordResetTokenService;
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/personal-details/{id}")
    public ResponseEntity<?> getUserByUserId(@PathVariable Long id, HttpServletResponse response,
            Authentication authentication) {
        if (id == null || id <= 0) {
            return Error.createResponse("Invalid request sent.", HttpStatus.BAD_REQUEST, "User Id is missing");
        } else if (userServices.getUserById(id) == null) {
            return Error.createResponse("User with ID " + id + " does not exist.", HttpStatus.BAD_REQUEST,
                    "User does not exist");
        }
        UserDTO userDTO = userRecordService.getUserDetailsById(id, authentication);
        
        return ResponseEntity.ok(userDTO);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/by/username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username, HttpServletResponse response,
            Authentication authentication) {
        if (username == null || username.isEmpty()) {
            return Error.createResponse("Username is require.*", HttpStatus.BAD_REQUEST, "Username is require.*");
        } else if (userServices.getUserByUsername(username) == null) {
            return Error.createResponse("User with username " + username + " does not exist.", HttpStatus.BAD_REQUEST,
                    "User does not exist");
        }
        UserDTO userDTO = userRecordService.getUserByUsername(username, authentication);

        return ResponseEntity.ok(userDTO);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/my-referral/{id}")
    public ResponseEntity<?> referral(@PathVariable Long id, Authentication authentication) {
        // get user referral code
        if (id == null || id <= 0) {
            return Error.createResponse("Invalid request sent.", HttpStatus.BAD_REQUEST, "User Id is missing");
        }

        else if (userServices.getUserById(id) == null) {
            return Error.createResponse("User with ID " + id + " does not exist.", HttpStatus.BAD_REQUEST,
                    "User does not exist");
        }
        Optional<Users> user = userServices.findById(id);
        String username = authentication.getName();
        if (!user.get().getUsername().equals(username)) {
            return Error.createResponse("Unauthorized access", HttpStatus.FORBIDDEN, "Access Danied");
        }
        return ResponseEntity.ok(userRecordService.getUserReferralCode(id));
    }

    @PreAuthorize("hasRole('USER')")
    @PutMapping("/settings/reset-password/{id}")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody UserSignUpRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(userServices.resetPassword(id, request.getPassword(), authentication));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PutMapping("/settings/deactivate-account/{id}")
    public ResponseEntity<?> deactivateAccount(@PathVariable Long id, Authentication authentication) {
        // deactivate account
        return ResponseEntity.ok(userServices.deactivateAccount(id, authentication));
    }

    @GetMapping("/referral-code/{id}")
    public ResponseEntity<?> referralCode(@PathVariable Long id, Authentication authentication) {
        // get referral code
        return ResponseEntity.ok(userRecordService.getUserReferralCode(id));
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(Authentication authentication) {
        String username = authentication.getName();
        Users user = userServices.getUserByUsername(username);
        if (!username.equals(user.getUsername())) {
            return Error.createResponse(
                    "UNAUTHORIZE ACCESS", HttpStatus.FORBIDDEN,
                    "You dont have access to the endpoints");
        }
        return ResponseEntity.ok(user);
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/settings/enable-twofactor")
    public ResponseEntity<?> verifyUserOtp(@RequestBody Map<String, Boolean> requestPayload,
            Authentication authentication) {
        Boolean enable2FA = requestPayload.get("enable2FA");

        if (enable2FA == null) {
            return ResponseEntity.badRequest().body("Invalid request payload");
        }
        try {

            return ResponseEntity.ok(twoFactorAuthenticationService.enableTwoFactorKey(enable2FA, authentication));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating Two-Factor Authentication");
        }
    }

    @PostMapping("/settings/update/user")
    public ResponseEntity<?> updateUser(
            @RequestParam(value = "profile", required = false) MultipartFile profile,
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("gender") String gender, Authentication authentication) {
        String requestUsername = authentication.getName();
        Users user = userServices.getUserByUsername(requestUsername);
        if (!username.equals(user.getUsername())) {
            return Error.createResponse(
                    "UNAUTHORIZE ACCESS", HttpStatus.FORBIDDEN,
                    "You dont have access to the endpoints");
        }
        try {
            String profilePath = null;

            // Handle file upload if present
            if (profile != null && !profile.isEmpty()) {
                // Ensure the upload directory exists
                Path uploadDir = Paths.get(fileStorageConfig.getUploadDir());
                if (Files.notExists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }

                // Replace the original file name with the user's ID
                String userId = user.getId().toString();
                String extension = StringUtils.getFilenameExtension(profile.getOriginalFilename());
                String newFileName = userId + (extension != null ? "." + extension : "");

                // Define the file path and save the file
                Path filePath = uploadDir.resolve(newFileName);

                // Check if the file already exists, and delete it if it does
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }

                // Copy the file to the destination
                Files.copy(profile.getInputStream(), filePath);

                // Set the profile path to be saved in the database
                String baseUrl = keysWrapper.getAssetUrl();
                profilePath = baseUrl + "/image/" + newFileName;
            }

            return ResponseEntity.ok(userServices.updateUserProfile(username, email, gender, profilePath));
        } catch (IOException e) {
            return new ResponseEntity<>("Failed to upload file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/settings/updatepassword")
    public ResponseEntity<?> updatePassword(@RequestBody ChangePasswordRequest request, Authentication authentication) {
        if (request.getPassword().isEmpty()) {
            return Error.createResponse("Password is require*", HttpStatus.BAD_REQUEST,
                    "Password can not be empty");
        }
        if (request.getConfirmPassword().isEmpty()) {
            return Error.createResponse("Confirm Password is require*", HttpStatus.BAD_REQUEST,
                    "Confirm Password can not be empty");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return Error.createResponse("Passwords do not match*", HttpStatus.BAD_REQUEST,
                    "Password and Confirm Password must be the same");
        }

        return ResponseEntity.ok(passwordResetTokenService.resetPassword(request, authentication));
    }

    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{id}/updateUserRecord/transferPin/status")
    public ResponseEntity<?> updateUserRecordTransferPinStatus(@PathVariable Long id, Authentication authentication) {
        if (id == null) {
            return Error.createResponse("User Id is requir.*", HttpStatus.BAD_REQUEST,
                    "Please provide you Id.");
        }
        return userRecordService.updateUserRecordTransferPinStatus(id, authentication);
    }

    @GetMapping("/by/public/username/{username}")
    public ResponseEntity<?> findPublicUserByUsername(@PathVariable String username, HttpServletResponse response) {
        if (username == null || username.isEmpty()) {
            return Error.createResponse("Username is require.*", HttpStatus.BAD_REQUEST, "Username is require.*");
        } else if (userServices.getUserByUsername(username) == null) {
            return Error.createResponse("User with username " + username + " does not exist.", HttpStatus.BAD_REQUEST,
                    "User does not exist");
        }
        UserDTO userDTO = userRecordService.findPublicUserByUsername(username);

        return ResponseEntity.ok(userDTO);
    }
    
    @GetMapping("/by/public/userId/{userId}")
    public ResponseEntity<?> findPublicUserByUserId(@PathVariable Long userId, HttpServletResponse response) {
        if (userId == null) {
            return Error.createResponse("User Id is require.*", HttpStatus.BAD_REQUEST, "Username is require.*");
        } else if (userServices.getUserById(userId) == null) {
            return Error.createResponse("User with user Id " + userId + " does not exist.", HttpStatus.BAD_REQUEST,
                    "User does not exist");
        }
        UserDTO userDTO = userRecordService.findPublicUserByUserId(userId);

        return ResponseEntity.ok(userDTO);
    }

    @PutMapping("/{id}/account/lock")
    public ResponseEntity<?> lockUserAccount(@PathVariable Long userId, HttpServletResponse response) {
        if (userId == null) {
            return Error.createResponse("User Id is require.*", HttpStatus.BAD_REQUEST, "Username is require.*");
        } else if (userServices.getUserById(userId) == null) {
            return Error.createResponse("User with user Id " + userId + " does not exist.", HttpStatus.BAD_REQUEST,
                    "User does not exist");
        }
    
        return userRecordService.lockUserAccount(userId);
    }

    @PutMapping("/{id}/account/block")
    public ResponseEntity<?> blockUserAccount(@PathVariable Long userId, HttpServletResponse response) {
        if (userId == null) {
            return Error.createResponse("User Id is require.*", HttpStatus.BAD_REQUEST, "Username is require.*");
        } else if (userServices.getUserById(userId) == null) {
            return Error.createResponse("User with user Id " + userId + " does not exist.", HttpStatus.BAD_REQUEST,
                    "User does not exist");
        }

        return userRecordService.blockUserAccount(userId);
    }

}
