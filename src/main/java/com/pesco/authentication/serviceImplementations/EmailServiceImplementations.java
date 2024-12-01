package com.pesco.authentication.serviceImplementations;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import com.pesco.authentication.MessageProducers.requests.OTPMessage;
import com.pesco.authentication.MessageProducers.requests.PasswordResetRequest;
import com.pesco.authentication.MessageProducers.requests.VerificationEmailRequest;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class EmailServiceImplementations{
    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;
    private static final String CHARACTERS = "4sah3bErz790123456789vdMZ1nsUQ";
    private static final int ID_LENGTH = 18;
    private static final Random random = new Random();

    public static String generateUniqueId() {
        StringBuilder sb = new StringBuilder(ID_LENGTH);
        for (int i = 0; i < ID_LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    @RabbitListener(queues = "email.verification")
    public void receiveVerificationEmail(VerificationEmailRequest emailRequest) {
        if (emailRequest != null) {
            sendEmailVerificationMessage(
                    emailRequest.getEmail(),
                    emailRequest.getContent(),
                    emailRequest.getLink(),
                    emailRequest.getUsername());
        } else {
            System.out.println("Failed to deserialize email request.");
        }
    }

    @Async
    public CompletableFuture<Void> sendEmailVerificationMessage(String email, String content, String link, String username) {
        // Create MimeMessage
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            // Create MimeMessageHelper
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, "utf-8");
            // Prepare the HTML template
            Context context = new Context();

            context.setVariable("username", username);
            context.setVariable("link", link);
            context.setVariable("content", content);

            String htmlContent = templateEngine.process("verification-email", context);

            // Set email attributes
            mimeMessageHelper.setTo(email);
            mimeMessageHelper.setSubject("Account Verification");
            mimeMessageHelper.setText(htmlContent, true);
            // Send the email
            javaMailSender.send(mimeMessage);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            throw new MailSendException("Failed to send email: " + e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "email.otp")
    public void receiveOTPEmail(OTPMessage otpRequest) {
        if (otpRequest !=null) {
            sendOTPMessage(
                otpRequest.getEmail(),
                otpRequest.getOtp(),
                otpRequest.getRestPassword(),
                otpRequest.getConfigTwoFactorAuth(),
                otpRequest.getConfigTwoFactorAuthRecovery()
            );
        } else {
            System.out.println("Failed to deserialize email request.");
        }
        
    }

    @Async
    public CompletableFuture<Void> sendOTPMessage(String email, String otp, String restPassword, String configTwoFactorAuth,
            String configTwoFactorAuthRecovery) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            // Create MimeMessageHelper
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, "utf-8");

            // Prepare the HTML template
            Context context = new Context();
            context.setVariable("otp", otp);
            context.setVariable("resetPassword", restPassword);
            context.setVariable("configuringTwoFactorAuthentication", configTwoFactorAuth);
            context.setVariable("configuringTwoFactorAuthenticationRecoveryMethods", configTwoFactorAuth);
            String htmlContent = templateEngine.process("verification-otp", context);

            // Set email attributes
            mimeMessageHelper.setTo(email);
            mimeMessageHelper.setSubject("Verify OTP");
            mimeMessageHelper.setText(htmlContent, true);
            // Send the email
            javaMailSender.send(mimeMessage);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            throw new MailSendException("Failed to send email: " + e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "email.reset-password")
    public void receivePasswordReset(PasswordResetRequest passwordResetRequest) {
        if(passwordResetRequest !=null){
            sendPasswordResetMessage(
                passwordResetRequest.getEmail(),
                passwordResetRequest.getUsername(),
                passwordResetRequest.getContent(),
                passwordResetRequest.getUrl(),
                passwordResetRequest.getCustomerEmail()
            );
        } else {
            System.out.println("Failed to deserialize email request.");
        }
    }

    @Async
    public CompletableFuture<Void> sendPasswordResetMessage(String email, String username, String content, String url, String customerEmail) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            // Create MimeMessageHelper
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, "utf-8");

            // Prepare the HTML template
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("link", url);
            context.setVariable("content", content);
            context.setVariable("supportEmail", customerEmail);
            String htmlContent = templateEngine.process("forget-password", context);

            // Set email attributes
            mimeMessageHelper.setTo(email);
            mimeMessageHelper.setSubject("Reset Password");
            mimeMessageHelper.setText(htmlContent, true);
            // Send the email
            javaMailSender.send(mimeMessage);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            throw new MailSendException("Failed to send email: " + e.getMessage(), e);
        }
    }
    

}
