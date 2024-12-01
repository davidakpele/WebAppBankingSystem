package com.example.deposit.serviceImplementations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import com.example.deposit.messageProducer.requests.CreditWalletNotification;
import com.example.deposit.messageProducer.requests.DebitWalletNotification;
import com.example.deposit.messageProducer.requests.DepositWalletNotification;
import com.example.deposit.messageProducer.requests.MaintenanceDeductionNotification;
import com.example.deposit.services.EmailService;
import com.example.deposit.utils.KeyCollections;
import com.example.deposit.utils.Refactory;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailServiceImplementations implements EmailService {
    
    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;
    private Long transactionId = KeyCollections.generateUniqueAuthorizeUserId();

    public String generateUniqueId() {
        long timestamp = System.currentTimeMillis();
        Random random = new Random();
        long randomNum = random.nextLong(1000000000000000L);
        return String.format("%014d%016d", timestamp, randomNum);
    }

    @RabbitListener(queues = "credit.wallet")
    public void receiveCreditWalletNotificationEmail(CreditWalletNotification creditWalletNotification) {
        if (creditWalletNotification != null) {
            sendCreditWalletNotificationToRecipient(
                    creditWalletNotification.getRecipientEmail(),
                    creditWalletNotification.getTransferAmount(),
                    creditWalletNotification.getSenderFullName(),
                    creditWalletNotification.getReceiverFullName(),
                    creditWalletNotification.getRecipientTotalBalance());
        } else {
            System.out.println("Failed to deserialize email request.");
        }
    }
    
    @Async
    public CompletableFuture<Void> sendCreditWalletNotificationToRecipient(String recipientEmail, BigDecimal transferAmount, String senderFullName, String receiverFullName, BigDecimal RecipientTotalBalance) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            // Create MimeMessageHelper
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, "utf-8");

            // Prepare the HTML template
            Context context = new Context();
            context.setVariable("name", receiverFullName);
            context.setVariable("senderName", senderFullName);
            context.setVariable("recipientName", receiverFullName);
    
            context.setVariable("transactionId", transactionId);
            context.setVariable("type", "CREDITED");
            context.setVariable("amount", Refactory.formatBigDecimal(transferAmount));
            context.setVariable("description", "Credited "+ Refactory.formatBigDecimal(transferAmount)+" by "+ senderFullName.toLowerCase());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedTime = LocalDateTime.now().format(formatter);
            context.setVariable("time", formattedTime);
            context.setVariable("totalBalance", Refactory.formatBigDecimal(RecipientTotalBalance));

            String htmlContent = templateEngine.process("TransactionNotification", context);

            // Set email attributes
            mimeMessageHelper.setTo(recipientEmail);
            mimeMessageHelper.setSubject("Transaction Details");
            mimeMessageHelper.setText(htmlContent, true);

            // Send the email
            javaMailSender.send(mimeMessage);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            throw new MailSendException("Failed to send email: " + e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "debit.wallet")
    public void receiveDebitWalletNotificationEmail(DebitWalletNotification debitWalletNotification) {
        if (debitWalletNotification != null) {
            sendDebitWalletNotificationToRecipient(
                    debitWalletNotification.getSenderEmail(),
                    debitWalletNotification.getFeeAmount(),
                    debitWalletNotification.getTransferAmount(),
                    debitWalletNotification.getSenderFullName(),
                    debitWalletNotification.getReceiverFullName(),
                    debitWalletNotification.getBalance()
            );
        } else {
            System.out.println("Failed to deserialize email request.");
        }
    }

    @Async
    public CompletableFuture<Void> sendDebitWalletNotificationToRecipient(String senderEmail, BigDecimal feeAmount,
            BigDecimal transferAmount, String senderFullName, String receiverFullName, BigDecimal balance) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            // Create MimeMessageHelper
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, "utf-8");

            // Prepare the HTML template
            Context context = new Context();
            context.setVariable("name", senderFullName);
            context.setVariable("recipientName", receiverFullName);
            context.setVariable("senderName", senderFullName);
            context.setVariable("transactionId", transactionId);
            context.setVariable("type", "DEBITED");
            context.setVariable("amount", Refactory.formatBigDecimal(transferAmount));
            context.setVariable("description","Transfered "+Refactory.formatBigDecimal(transferAmount)+" to "+ receiverFullName.toLowerCase());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedTime = LocalDateTime.now().format(formatter);
            context.setVariable("time", formattedTime);
            context.setVariable("totalBalance", Refactory.formatBigDecimal(balance));

            String htmlContent = templateEngine.process("TransactionNotification", context);

            // Set email attributes
            mimeMessageHelper.setTo(senderEmail);
            mimeMessageHelper.setSubject("Transaction Details");
            mimeMessageHelper.setText(htmlContent, true);

            // Send the email
            javaMailSender.send(mimeMessage);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            throw new MailSendException("Failed to send email: " + e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "deposit.wallet")
    public void receiveDepositWalletNotificationEmail(DepositWalletNotification depositWalletNotification) {
        if (depositWalletNotification != null) {
            sendDepositNotification(
                depositWalletNotification.getRecipientEmail(),
                depositWalletNotification.getRecipientName(),
                depositWalletNotification.getDepositAmount(),
                depositWalletNotification.getTransactionTime(),
                depositWalletNotification.getTotalBalance()
            );
        } else {
            System.out.println("Failed to deserialize email request.");
        }
    }

    @Async
    public CompletableFuture<Void> sendDepositNotification(String recipientEmail, String recipientName, BigDecimal depositAmount,
            LocalDateTime transactionTime, BigDecimal totalBalance) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            // Create MimeMessageHelper
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, "utf-8");

            // Prepare the HTML template
            Context context = new Context();
            context.setVariable("username", recipientName);
            context.setVariable("amount", Refactory.formatBigDecimal(depositAmount));
            context.setVariable("transactionId", generateUniqueId());

            // Format the time
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy hh:mm a");
            String formattedTime = transactionTime.format(formatter);
            context.setVariable("transactionTime", formattedTime);

            context.setVariable("totalBalance", Refactory.formatBigDecimal(totalBalance));

            String htmlContent = templateEngine.process("DepositNotification", context);

            // Set email attributes
            mimeMessageHelper.setTo(recipientEmail);
            mimeMessageHelper.setSubject("Deposit Successful");
            mimeMessageHelper.setText(htmlContent, true);

            // Send the email
            javaMailSender.send(mimeMessage);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            throw new MailSendException("Failed to send email: " + e.getMessage(), e);
        }
    }


    @RabbitListener(queues = "deposit.wallet")
    public void receiveMaintenanceDeductionNotificationEmail(MaintenanceDeductionNotification maintenanceDeductionNotification) {
        if (maintenanceDeductionNotification != null) {
            sendMaintenanceDeductionNotification(
                    maintenanceDeductionNotification.getEmail(),
                    maintenanceDeductionNotification.getFirstName(),
                    maintenanceDeductionNotification.getAmount(),
                    maintenanceDeductionNotification.getBalance(),
                    maintenanceDeductionNotification.getContent());
        } else {
            System.out.println("Failed to deserialize email request.");
        }
    }


    @Async
    public CompletableFuture<Void> sendMaintenanceDeductionNotification(String email, String firstName, BigDecimal amount, BigDecimal balance, String content) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            // Create MimeMessageHelper
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, "utf-8");

            // Prepare the HTML template
            Context context = new Context();
            context.setVariable("username", firstName);
            context.setVariable("amount", amount);
            // Format the time
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy hh:mm a");
            LocalDateTime transactionTime = LocalDateTime.now();
            String formattedTime = transactionTime.format(formatter);
            context.setVariable("transactionTime", formattedTime);

            context.setVariable("balance", Refactory.formatBigDecimal(balance));

            String htmlContent = templateEngine.process("MaintenanceDeductionNotification", context);

            // Set email attributes
            mimeMessageHelper.setTo(email);
            mimeMessageHelper.setSubject("Monthly Maintenance Fee Deduction");
            mimeMessageHelper.setText(htmlContent, true);

            // Send the email
            javaMailSender.send(mimeMessage);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            throw new MailSendException("Failed to send email: " + e.getMessage(), e);
        }
    }
}
