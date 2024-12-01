package com.example.deposit.messageProducer.requests;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MaintenanceDeductionNotification {
    private String email;
    private String firstName;
    private BigDecimal amount;
    private BigDecimal balance;
    private String content;

    public MaintenanceDeductionNotification() {
    }

     @JsonCreator
    public MaintenanceDeductionNotification(
            @JsonProperty("email") String email,
            @JsonProperty("firstName") String firstName,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("balance") BigDecimal balance,
            @JsonProperty("content") String content) {
        this.email = email;
        this.firstName = firstName;
        this.amount = amount;
        this.balance = balance;
        this.content = content;
    }


    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalance() {
        return this.balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }


    

}
