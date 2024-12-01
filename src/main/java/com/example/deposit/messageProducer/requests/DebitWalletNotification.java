package com.example.deposit.messageProducer.requests;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DebitWalletNotification {
    private String senderEmail; 
    private BigDecimal feeAmount;
    private BigDecimal transferAmount;
    private String senderFullName;
    private String receiverFullName;
    private BigDecimal balance;

    public DebitWalletNotification() {
    }

    // Constructor to map from JSON
    @JsonCreator
    public DebitWalletNotification(
            @JsonProperty("senderEmail") String senderEmail,
            @JsonProperty("feeAmount") BigDecimal feeAmount,
            @JsonProperty("transferAmount") BigDecimal transferAmount,
            @JsonProperty("senderFullName") String senderFullName,
            @JsonProperty("receiverFullName") String receiverFullName,
            @JsonProperty("balance") BigDecimal balance) {
        this.senderEmail = senderEmail;
        this.feeAmount = feeAmount;
        this.transferAmount = transferAmount;
        this.senderFullName = senderFullName;
        this.receiverFullName = receiverFullName;
        this.balance = balance;
    }

    public String getSenderEmail() {
        return this.senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public BigDecimal getFeeAmount() {
        return this.feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public BigDecimal getTransferAmount() {
        return this.transferAmount;
    }

    public void setTransferAmount(BigDecimal transferAmount) {
        this.transferAmount = transferAmount;
    }

    public String getSenderFullName() {
        return this.senderFullName;
    }

    public void setSenderFullName(String senderFullName) {
        this.senderFullName = senderFullName;
    }

    public String getReceiverFullName() {
        return this.receiverFullName;
    }

    public void setReceiverFullName(String receiverFullName) {
        this.receiverFullName = receiverFullName;
    }

    public BigDecimal getBalance() {
        return this.balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

   
    
    
}
