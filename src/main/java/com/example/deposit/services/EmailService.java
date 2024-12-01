package com.example.deposit.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

public interface EmailService {
    CompletableFuture<Void> sendCreditWalletNotificationToRecipient(String recipientEmail, BigDecimal transferAmount,String senderFullName, String receiverFullName, BigDecimal RecipientTotalBalance);
    
    CompletableFuture<Void> sendDebitWalletNotificationToRecipient(String senderEmail, BigDecimal feeAmount, BigDecimal transferAmount, String senderFullName, String receiverFullName, BigDecimal balance);

    CompletableFuture<Void> sendDepositNotification(String recipientEmail, String recipientName, BigDecimal depositAmount, LocalDateTime transactionTime, BigDecimal totalBalance);
}
