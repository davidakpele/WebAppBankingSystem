package com.example.deposit.messageProducer.requests;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CreditWalletNotification {
    private String recipientEmail;
    private BigDecimal transferAmount;
    private String senderFullName;
    private String receiverFullName;
    private BigDecimal recipientTotalBalance;

    // Default constructor
    public CreditWalletNotification() {
    }

    // Constructor to map from JSON
    @JsonCreator
    public CreditWalletNotification(
            @JsonProperty("recipientEmail") String recipientEmail,
            @JsonProperty("transferAmount") BigDecimal transferAmount,
            @JsonProperty("senderFullName") String senderFullName,
            @JsonProperty("receiverFullName") String receiverFullName,
            @JsonProperty("recipientTotalBalance") BigDecimal recipientTotalBalance) {
        this.recipientEmail = recipientEmail;
        this.transferAmount = transferAmount;
        this.senderFullName = senderFullName;
        this.receiverFullName = receiverFullName;
        this.recipientTotalBalance = recipientTotalBalance;
    }

   
    public String getRecipientEmail() {
        return this.recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
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

    public BigDecimal getRecipientTotalBalance() {
        return this.recipientTotalBalance;
    }

    public void setRecipientTotalBalance(BigDecimal recipientTotalBalance) {
        this.recipientTotalBalance = recipientTotalBalance;
    }

}
