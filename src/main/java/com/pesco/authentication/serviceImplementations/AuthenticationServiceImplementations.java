package com.pesco.authentication.serviceImplementations;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pesco.authentication.MessageProducers.NotificationProducer;
import com.pesco.authentication.enums.AttemptType;
import com.pesco.authentication.enums.Role;
import com.pesco.authentication.enums.UserStatus;
import com.pesco.authentication.micro_services.CoinWalletService;
import com.pesco.authentication.models.AuthorizeUserVerification;
import com.pesco.authentication.models.TwoFactorAuthentication;
import com.pesco.authentication.models.UserRecord;
import com.pesco.authentication.models.Users;
import com.pesco.authentication.models.VerificationToken;
import com.pesco.authentication.payloads.UserSignUpRequest;
import com.pesco.authentication.payloads.UserSignInRequest;
import com.pesco.authentication.properties.KeysWrapper;
import com.pesco.authentication.repositories.AuthorizeUserVerificationRepository;
import com.pesco.authentication.repositories.UserRecordRepository;
import com.pesco.authentication.repositories.UsersRepository;
import com.pesco.authentication.repositories.VerificationTokenRepository;
import com.pesco.authentication.responses.AuthenticationResponse;
import com.pesco.authentication.responses.VerificationTokenResult;
import com.pesco.authentication.services.AuthenticationService;
import com.pesco.authentication.services.AuthorizeUserVerificationService;
import com.pesco.authentication.services.JwtService;
import com.pesco.authentication.services.UserAttemptService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthenticationServiceImplementations implements AuthenticationService {
    
    private static final int EXPIRATION_MINUTES = 10;
    private Date expirationTime;
    private final CoinWalletService coinWalletService;
    private final UsersRepository userRepository;
    private final UserRecordRepository userRecordRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationProducer notificationProducer;
    private final VerificationTokenRepository verificationTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final KeysWrapper keysWrapper;
    private final AuthorizeUserVerificationService authorizeUserVerificationService;
    private final AuthorizeUserVerificationRepository authorizeUserVerificationRepository;
    private final TwoFactorAuthenticationServiceImplementations twoFactorAuthenticationServiceImplementation;
    private final UserAttemptService userAttemptService;

    @Transactional
    public ResponseEntity<String> createAccount(UserSignUpRequest request) {
        Long nextUserId = getNextUserId();

        Users user = Users.builder()
            .id(nextUserId)
            .email(request.getEmail())
            .username(request.getUsername())
            .twoFactorAuth(false)
            .role(Role.USER)
            .password(passwordEncoder.encode(request.getPassword()))
            .build();
        userRepository.save(user);

        // Retrieve the saved user to get the user ID
        Users savedUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationServiceException("User not found"));
        // generate referral code
        String referralCode = UUID.randomUUID().toString();

        UserRecord userRecord = UserRecord.builder()
            .user(savedUser)
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .gender(request.getGender())
            .telephone(request.getTelephone())
            .is_transfer_pin(false)
            .locked(false)
            .locked_at(LocalDateTime.now())
            .referral_code(referralCode)
            .is_blocked(false)
            .total_refs(null)
            .notifications(String.valueOf('0'))
            .referral_link(keysWrapper.getUrl() + "/auth/register?referral_code=" + referralCode)
            .status(UserStatus.PENDING_VERIFICATION)
            .build();
        userRecordRepository.save(userRecord);

        // Create a verification token
        UUID verificationToken = UUID.randomUUID();
        expirationTime = calculateExpirationDate(EXPIRATION_MINUTES);
        VerificationToken tokenEntity = VerificationToken.builder()
                .userId(nextUserId)
                .token(String.valueOf(verificationToken))
                .expirationTime(expirationTime)
                .build();
        verificationTokenRepository.save(tokenEntity);
        Long unverifiedUserId = KeysWrapper.generateUniqueAuthorizeUserId();
        authorizeUserVerificationService.save(nextUserId, unverifiedUserId);
        // Generate the verification link using keysWrapper.getUrl()
        String verificationLink = keysWrapper.getUrl() + "/auth/verifyRegistration?token=" + verificationToken+"&id="+unverifiedUserId;

        String content = "Dear " + user.getUsername() + ",\n\n"
                + "Thank you for signing up for pesco! We're excited to have you on board.\n\n"
                + "Please verify your email address to complete your registration and activate your account.";
                
        notificationProducer.sendVerificationEmail(savedUser.getEmail(), content, verificationLink, request.getUsername());
        String message = "Thanks for your interest in joining Artex network! To complete account verification, email has been sent to email address you provided.";
        return new ResponseEntity<>(message, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<?> login(UserSignInRequest request, HttpServletResponse response) {
        Map<String, Object> Authresponse = new HashMap<>();

        Optional<Users> userInfo = userRepository.findByUsername(request.getUsername());
        if (userInfo.get().isEnabled()) {
            try {
                authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                        request.getUsername(), 
                        request.getPassword()
                    )
                );
                Users user = userRepository.findById(userInfo.get().getId())
                        .orElseThrow(() -> new AuthenticationServiceException("User records not found"));

                Optional<UserRecord> checkAccountStatus = userRecordRepository.findByUserId(user.getId());
                if (checkAccountStatus.isPresent()) {
                    if (checkAccountStatus.get().isLocked()) {
                        // Account is not verified
                        Authresponse.put("status", HttpStatus.BAD_REQUEST);
                        Authresponse.put("success", false);
                        Authresponse.put("message",
                                "Sorry, This account is currently Locked, Please contact our customer service.");

                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Authresponse);
                    } else if (checkAccountStatus.get().is_blocked()) {
                        // Account is not verified
                        Authresponse.put("status", false);
                        Authresponse.put("success", false);
                        Authresponse.put("message",
                                "Sorry, This account is currently Blocked, Please contact our customer service.");

                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Authresponse);
                    }
                }
                var jwtToken = jwtService.generateToken((UserDetails) userInfo.get());

                boolean isTwoFactorAuthEnabled = user.isTwoFactorAuth();
                Optional<Users> authUser = userRepository.findByEmail(user.getEmail());

                // Handle Two-Factor Authentication
                if (isTwoFactorAuthEnabled) {
                    Authresponse.put("message", "Two factor authentication is enabled.");
                    Authresponse.put("twoFactorAuthEnabled", isTwoFactorAuthEnabled);
                    Authresponse.put("status", HttpStatus.BAD_REQUEST);

                    String Otp = keysWrapper.generateOTP();
                    String jwt = keysWrapper.generateUniqueKey();

                    TwoFactorAuthentication alreadyExistTwoFactorOTP = twoFactorAuthenticationServiceImplementation
                            .findByUser(user.getId());

                    if (alreadyExistTwoFactorOTP != null) {
                        twoFactorAuthenticationServiceImplementation.deleteTwoFactorOtp(alreadyExistTwoFactorOTP);
                    }

                    Users userOtp = authUser.get();
                    TwoFactorAuthentication newTwoFactorOTP = twoFactorAuthenticationServiceImplementation
                            .createTwoFactorOtp(userOtp, Otp, jwt);

                    Authresponse.put("status", newTwoFactorOTP.getId().toString());

                    TwoFactorAuthentication getToken = twoFactorAuthenticationServiceImplementation
                            .findByUser(user.getId());

                    Authresponse.put("jwt", getToken.getJwt());

                    String restPassword = keysWrapper.getUrl() + "/auth/security/password";
                    String configTwoFactorAuth = keysWrapper.getUrl()
                            + "/auth/security/configuring-two-factor-authentication";
                    String configTwoFactorAuthRecovery = keysWrapper.getUrl()
                            + "/auth/security/configuring-two-factor-authentication-recovery-methods";

                    // Send otp to user email
                    notificationProducer.sendOptEmail(user.getEmail(), Otp, restPassword, configTwoFactorAuth,
                            configTwoFactorAuthRecovery);

                    return new ResponseEntity<>(Authresponse, HttpStatus.OK);
                }

                Authresponse.put("jwt", jwtToken);
                Authresponse.put("email", userInfo.get().getEmail());
                Authresponse.put("userId", userInfo.get().getId());
                Authresponse.put("status", HttpStatus.OK);
                Authresponse.put("success", true);
                Authresponse.put("session", true);
                Authresponse.put("twoFactorAuthEnabled", userInfo.get().isTwoFactorAuth());
                Authresponse.put("username", userInfo.get().getUsername());
                // Set the JWT as a cookie
                Cookie cookie = new Cookie("authToken", jwtToken);
                cookie.setMaxAge(24 * 60 * 60);
                cookie.setPath("/");
                response.addCookie(cookie);

                return new ResponseEntity<>(Authresponse, HttpStatus.OK);
            } catch (Exception e) {
                // Authentication failed, increment bad attempts counter
                ResponseEntity<?> createAttempt = userAttemptService.createFailAttempt(userInfo.get().getId(), AttemptType.LOGIN);
                
                Authresponse.put("status", HttpStatus.BAD_REQUEST);
                Authresponse.put("success", false);
                Authresponse.put("details", createAttempt.getBody() == null ? "Invalid credentials provided.!":createAttempt.getBody().toString());
               
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Authresponse);
            }
        } else {
            // Account is not verified
            Authresponse.put("status", false);
            Authresponse.put("success", false);
            Authresponse.put("message", "This account has not been verified");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Authresponse);
        }
        
    }

    @Override
    public Optional<Users> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<Users> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public VerificationTokenResult generateVerificationToken(String oldToken) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(oldToken);
        if (verificationToken == null) {
            return new VerificationTokenResult(false, "Token not found");
        }

        // Update expirationTime (adjust this based on your requirements)
        Date newExpirationTime = calculateExpirationDate(EXPIRATION_MINUTES);

        Optional<Users> user = userRepository.findById(verificationToken.getUserId());
        verificationToken.setExpirationTime(newExpirationTime);

        String newToken = UUID.randomUUID().toString();
        // Generate a new token
        verificationToken.setToken(newToken);

        // Save the updated verification token
        verificationTokenRepository.save(verificationToken);

        Optional<AuthorizeUserVerification> optionAuthUser = authorizeUserVerificationRepository.findUserByIdOptional(user.get().getId());
        
        String verificationLink = keysWrapper.getUrl() + "/auth/verifyRegistration?token=" + newToken + "&id="
                + optionAuthUser.get().getId();

        String content = "Dear " + user.get().getUsername() + ",\n\n"
                + "Thank you for registering with Pesco! We're thrilled to have you join us.\n\n"
                + "To complete your registration and activate your account";
        notificationProducer.sendVerificationEmail(user.get().getEmail(), content, verificationLink, user.get().getUsername());
        return new VerificationTokenResult(true, verificationToken);
    }
    
    private Date calculateExpirationDate(int expirationMinutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(new Date().getTime());
        calendar.add(Calendar.MINUTE, expirationMinutes);
        return new Date(calendar.getTime().getTime());
    }

    @Override
    @Transactional
    public ResponseEntity<?> verifyUser(String token, Long id) {
        AuthenticationResponse verifyResponse = new AuthenticationResponse();
        // find user first
        boolean checkVerifyUser = authorizeUserVerificationRepository.findUserById(id);
        if(checkVerifyUser){
            verifyResponse.setMessage("This account has already been verified.");
            verifyResponse.setStatus(true);
            return new ResponseEntity<>(verifyResponse, HttpStatus.OK);
        }
        // Find the verification token
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token);
        
        // Check if the token is null
        if (verificationToken == null) {
            verifyResponse.setMessage("Invalid verification token");
            verifyResponse.setStatus(false);
            return new ResponseEntity<>(verifyResponse, HttpStatus.BAD_REQUEST);
        }

        // Retrieve the user associated with the verification token
        Optional<Users> optionalUser = userRepository.findById(verificationToken.getUserId().longValue());

        if (optionalUser.isPresent()) {
            Users user = optionalUser.get();
            // send request to create crypto wallet for user
           
            CompletableFuture.runAsync(() -> coinWalletService.createUserWallet(user))
                    .exceptionally(ex -> handleAsyncError("Failed to create wallet for user.", ex));

            // Check if the user account is already verified
            if (user.isEnabled()) {
                verifyResponse.setMessage("Hi " + user.getUsername() + ", Your account has already been verified.");
                verifyResponse.setStatus(true);
                return new ResponseEntity<>(verifyResponse, HttpStatus.OK);
            }
        }

        Calendar cal = Calendar.getInstance();
        if (verificationToken.getExpirationTime().getTime() - cal.getTime().getTime() < 0) {
            verifyResponse.setMessage("Verification token has expired, Click the resend button to have a new token assign to you.");
            verifyResponse.setStatus(false);
            return new ResponseEntity<>(verifyResponse, HttpStatus.CONFLICT);
        }

        if (optionalUser.isPresent()) {
            // Retrieve the user from the Optional
            Users user = optionalUser.get();
            Optional<UserRecord> optionalRecord  = userRecordRepository.findByUserId(optionalUser.get().getId());
            if (optionalRecord.isPresent()) {
                UserRecord updateRecord = optionalRecord.get();
                updateRecord.setStatus(UserStatus.ACTIVE);
                updateRecord.setLocked(false);
                updateRecord.set_blocked(false);
                userRecordRepository.save(updateRecord);
            }
            // Enable the user account
            user.setEnabled(true);
            userRepository.save(user);

            // Delete the verification token
            verificationTokenRepository.delete(verificationToken);

            verifyResponse.setMessage("User registration verified successfully");
            verifyResponse.setStatus(true);
            return new ResponseEntity<>(verifyResponse, HttpStatus.OK);
        }

        AuthenticationResponse notFoundResponse = new AuthenticationResponse();
        notFoundResponse.setMessage("User not found.");
        notFoundResponse.setStatus(false);
        return new ResponseEntity<>(notFoundResponse, HttpStatus.BAD_REQUEST);
    }
    
    private Long getNextUserId() {
        List<Users> existingUsers = userRepository.findAll();
        Users newUser = new Users();
        if (existingUsers.isEmpty()) {
            newUser.setId(1001L);
        } else {
            // Find the maximum existing user ID
            Long maxId = existingUsers.stream()
                    .map(Users::getId)
                    .max(Long::compare)
                    .orElse(0L);
            // Set the new user ID
            newUser.setId(maxId + 1);
        }
        return newUser.getId();
    }
    
    private Void handleAsyncError(String message, Throwable ex) {

        return null;
    }
}
