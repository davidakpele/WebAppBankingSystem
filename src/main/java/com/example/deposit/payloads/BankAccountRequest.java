package com.example.deposit.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BankAccountRequest {
    private String accountNumber;
    private String bankCode;
    private String bankName;
    private String account_holder_name;
}
