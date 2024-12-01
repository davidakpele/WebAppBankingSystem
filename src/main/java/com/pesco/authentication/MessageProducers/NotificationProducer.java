package com.pesco.authentication.MessageProducers;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import com.pesco.authentication.MessageProducers.requests.OTPMessage;
import com.pesco.authentication.MessageProducers.requests.PasswordResetRequest;
import com.pesco.authentication.MessageProducers.requests.VerificationEmailRequest;
import lombok.RequiredArgsConstructor;
 
@Service
@RequiredArgsConstructor
public class NotificationProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendVerificationEmail(String email, String content, String link, String username) {
        VerificationEmailRequest emailRequest = new VerificationEmailRequest(email, content, link, username);
        rabbitTemplate.convertAndSend("auth.notifications", "email.verification", emailRequest);
    }
    
    public void sendOptEmail(String email, String otp, String restPassword, String configTwoFactorAuth, String configTwoFactorAuthRecovery) {
        OTPMessage otpMessage = new OTPMessage(email, otp, restPassword, configTwoFactorAuth,configTwoFactorAuthRecovery);
        rabbitTemplate.convertAndSend("auth.notifications", "email.otp", otpMessage);
    }
    
    public void sendPasswordResetEmail(String email, String username, String content, String url, String customerEmail) {
        PasswordResetRequest passwordResetRequest = new PasswordResetRequest(email, username, content, url, customerEmail);
        rabbitTemplate.convertAndSend("auth.notifications", "email.reset-password", passwordResetRequest);
    }

}
