package com.example.deposit.payloads;

import java.math.BigDecimal;

import com.example.deposit.enums.CurrencyType;
import com.example.deposit.enums.DEPOSITANDWITHDRAWALSYSTEM;
import com.example.deposit.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DepositRequest {
    private String accountHolderName;
    private String accountNumber;
    private String bankCode;
    private String bankName;
    private String userId;
    private String walletId;
    private BigDecimal amount;
    private TransactionType type;
    private CurrencyType currencyType;
    private String service;
    private DEPOSITANDWITHDRAWALSYSTEM depositSystem;
}