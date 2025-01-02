package com.pesco.authentication.controllers;

import java.util.Optional;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import com.pesco.authentication.models.Users;
import com.pesco.authentication.payloads.UserSignUpRequest;
import com.pesco.authentication.payloads.ChangePasswordRequest;
import com.pesco.authentication.payloads.UserSignInRequest;
import com.pesco.authentication.payloads.OTPRequest;
import com.pesco.authentication.responses.VerificationTokenResult;
import com.pesco.authentication.serviceImplementations.AuthenticationServiceImplementations;
import com.pesco.authentication.serviceImplementations.PasswordResetTokenServiceImplementation;
import com.pesco.authentication.serviceImplementations.TwoFactorAuthenticationServiceImplementations;
import com.pesco.authentication.serviceImplementations.UserServiceImplementations;
import com.pesco.authentication.responses.AuthenticationResponse;
import com.pesco.authentication.responses.Error;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationServiceImplementations authenticationService;
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$";
    private final UserServiceImplementations userServiceImplementation;
    private final PasswordResetTokenServiceImplementation passwordResetTokenServiceImplementation;
    private final TwoFactorAuthenticationServiceImplementations twoFactorAuthenticationServiceImplementation;

    public AuthController(
            AuthenticationServiceImplementations authenticationService,
            UserServiceImplementations userServiceImplementation,
            PasswordResetTokenServiceImplementation passwordResetTokenServiceImplementation,
            TwoFactorAuthenticationServiceImplementations twoFactorAuthenticationServiceImplementation) {
        this.authenticationService = authenticationService;
        this.userServiceImplementation = userServiceImplementation;
        this.passwordResetTokenServiceImplementation = passwordResetTokenServiceImplementation;
        this.twoFactorAuthenticationServiceImplementation = twoFactorAuthenticationServiceImplementation;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserSignUpRequest request) {
        if (request.getFirstName().isEmpty() || request.getFirstName() ==null) {
            return Error.createResponse("Firstname is require.*", HttpStatus.BAD_REQUEST,
                    "Firstname can not be empty");
        }
        if (request.getLastName().isEmpty() || request.getLastName() ==null) {
            return Error.createResponse("Lastname is require.*", HttpStatus.BAD_REQUEST,
                    "Lastname can not be empty");
        }

        if (request.getUsername().isEmpty() || request.getUsername() ==null) {
            return Error.createResponse("Username is require.*", HttpStatus.BAD_REQUEST,
                    "Username can not be empty");
        }

        else if (existUsername(request.getUsername())) {
            return Error.createResponse("Sorry..! Username already been choosen by another user.*",
                    HttpStatus.BAD_REQUEST, "This Username has been used.");
        }

        if (request.getEmail().isEmpty() || request.getEmail() ==null) {
            return Error.createResponse("Eamil is require.*", HttpStatus.BAD_REQUEST,
                    "Email can not be empty");
        }

        if (request.getGender().isEmpty() || request.getGender() == null) {
            return Error.createResponse("Gender is require.*", HttpStatus.BAD_REQUEST,
                    "Gender can not be empty");
        }
        if (request.getEmail().isEmpty() || request.getEmail() ==null) {
            return Error.createResponse("Email Address is require.*", HttpStatus.BAD_REQUEST, "Invalid email address");
        }
        // Check if the email is of valid format using regex
        else if (!request.getEmail().matches(EMAIL_REGEX)) {
            return Error.createResponse("Invalid email format*", HttpStatus.BAD_REQUEST, "Invalid email address");
        }
        // Check if email already exists
        else if (emailExists(request.getEmail())) {
            return Error.createResponse("Sorry..! Email already been used by another user.*", HttpStatus.BAD_REQUEST,
                    "This email has been used.");
        }

        if (request.getTelephone().isEmpty() || request.getTelephone() ==null) {
            return Error.createResponse("Telephone is require*", HttpStatus.BAD_REQUEST, "Invalid email address");
        }

        if (request.getPassword().isEmpty() || request.getPassword() ==null) {
            return Error.createResponse("Password is require.*", HttpStatus.BAD_REQUEST, "Password can not be empty");
        }
        return authenticationService.createAccount(request);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserSignInRequest request,
            HttpServletResponse response) {
        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            return Error.createResponse("Username is require.*", HttpStatus.BAD_REQUEST, "Username can not be empty");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            return Error.createResponse("Password is require*", HttpStatus.BAD_REQUEST, "Password can not be empty");
        }
        return authenticationService.login(request, response);
    }

    @GetMapping("/verifyRegistration")
    public ModelAndView verifyRegistration(@RequestParam("token") String token, @RequestParam("id") Long id) {
        if (token == null || token.isEmpty() || id == null) {
            ModelAndView modelAndView = new ModelAndView("error");
            modelAndView.addObject("message", "Missing 'token' or 'id' parameter.");
            modelAndView.setStatus(HttpStatus.BAD_REQUEST);
            return modelAndView;
        }

        ResponseEntity<?> response = authenticationService.verifyUser(token, id);
        AuthenticationResponse authResponse = (AuthenticationResponse) response.getBody();

        ModelAndView modelAndView;

        // Handle the case where the response status is OK
        if (response.getStatusCode() == HttpStatus.OK) {
            modelAndView = new ModelAndView("success");
            modelAndView.addObject("message", "This account has been verified.");
        } else {
            // If the token is expired, show the error page with the resend button
            if (response.getStatusCode() == HttpStatus.CONFLICT) {
                modelAndView = new ModelAndView("error");
                modelAndView.addObject("message", "Verification token has expired.");
                modelAndView.addObject("showResendButton", true);
                modelAndView.addObject("token", token);
                modelAndView.setStatus(HttpStatus.BAD_REQUEST);
                return modelAndView;
            }

            // If some other error occurs, show a generic error page
            modelAndView = new ModelAndView("error");
            modelAndView.addObject("message",
                    authResponse != null ? authResponse.getMessage() : "An unexpected error occurred.");
            modelAndView.setStatus(response.getStatusCode());
        }

        return modelAndView;
    }

    @GetMapping("/resendVerifyToken")
    public ResponseEntity<?> resendVerificationToken(@RequestParam("token") String oldToken) {
        VerificationTokenResult response = authenticationService.generateVerificationToken(oldToken);

        if (response.equals("failure")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } else {
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/forget-password")
    public ResponseEntity<?> forgetPassword(@RequestBody UserSignUpRequest request) {
        if (request.getEmail().isEmpty()) {
            return Error.createResponse("Email address is require*", HttpStatus.BAD_REQUEST,
                    "Provide your email address.");
        } else if (request.getEmail() == null) {
            return Error.createResponse("Your need to provide email address", HttpStatus.BAD_REQUEST,
                    "Invalid request sent.");
        }
        return userServiceImplementation.forgetPassword(request.getEmail());
    }

    @GetMapping("/reset-password")
    public ResponseEntity<?> showResetPasswordPage(@RequestParam("token") String token) {
        if (token == null || token.isEmpty()) {
            return Error.createResponse("Valid token parameter is require.", HttpStatus.BAD_REQUEST,
                    "Provide token parameter to validate this endpoint.");
        } else {
            return passwordResetTokenServiceImplementation.findByToken(token);
        }
    }

    @PostMapping("/create-new-password")
    public ResponseEntity<?> createNewPassword(@RequestBody ChangePasswordRequest request) {
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

        boolean isUpdated = passwordResetTokenServiceImplementation.updatePassword(request);

        if (isUpdated) {
            return new ResponseEntity<>("Password successfully updated.", HttpStatus.CREATED);
        } else {
            return Error.createResponse("Invalid or expired token", HttpStatus.BAD_REQUEST,
                    "The token is invalid or has expired");
        }
    }

    @GetMapping("/verify-otp-token")
    public ResponseEntity<?> verifyOtpToken(@RequestParam("token") String token) {
        if (token == null || token.isEmpty()) {
            return Error.createResponse("Valid token parameter is require.", HttpStatus.BAD_REQUEST,
                    "Provide token parameter to validate this endpoint.");
        } else {
            return twoFactorAuthenticationServiceImplementation.findByToken(token);
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyUserOtp(@RequestBody OTPRequest reqOtpPayload) {
        if (reqOtpPayload.getOtp() == null || reqOtpPayload.getOtp().isEmpty()) {
            return Error.createResponse("OTP require*.", HttpStatus.BAD_REQUEST,
                    "Provide the OTP code sent to your email address");
        } else {
            return twoFactorAuthenticationServiceImplementation.verifyUserTwoFactorOtp(reqOtpPayload);
        }
    }

    private boolean emailExists(String email) {
        Optional<Users> existingUsers = authenticationService.findByEmail(email);
        return existingUsers.isPresent();
    }

    private boolean existUsername(String username) {
        Optional<Users> existingUsers = authenticationService.findByUsername(username);
        return existingUsers.isPresent();
    }

    @GetMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        Cookie authTokenCookie = new Cookie("jwt", null);
        authTokenCookie.setMaxAge(0);
        authTokenCookie.setPath("/");
        response.addCookie(authTokenCookie);
        return ResponseEntity.ok("Logged out successfully");
    }

}
