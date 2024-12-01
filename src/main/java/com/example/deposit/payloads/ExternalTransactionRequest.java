package com.example.deposit.payloads;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExternalTransactionRequest {
    private String accountNumber;
    private BigDecimal amount;
    private String bankcode;
    private String description;
    private String fromUser;
    private String transferpin;
}
