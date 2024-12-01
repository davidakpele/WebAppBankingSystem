package com.example.deposit.messageProducer.requests;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DepositWalletNotification {
    private String recipientEmail; 
    private String recipientName;
    private BigDecimal depositAmount;
    private LocalDateTime transactionTime;
    private BigDecimal totalBalance;

    public DepositWalletNotification() {
    }

    @JsonCreator
    public DepositWalletNotification(
            @JsonProperty("recipientEmail") String recipientEmail,
            @JsonProperty("recipientName") String recipientName,
            @JsonProperty("depositAmount") BigDecimal depositAmount,
            @JsonProperty("transactionTime") LocalDateTime transactionTime,
            @JsonProperty("totalBalance") BigDecimal totalBalance) {
        this.recipientEmail = recipientEmail;
        this.recipientName = recipientName;
        this.depositAmount = depositAmount;
        this.transactionTime = transactionTime;
        this.totalBalance = totalBalance;
    }


    public String getRecipientEmail() {
        return this.recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getRecipientName() {
        return this.recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public BigDecimal getDepositAmount() {
        return this.depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public LocalDateTime getTransactionTime() {
        return this.transactionTime;
    }

    public void setTransactionTime(LocalDateTime transactionTime) {
        this.transactionTime = transactionTime;
    }

    public BigDecimal getTotalBalance() {
        return this.totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }

    
}
