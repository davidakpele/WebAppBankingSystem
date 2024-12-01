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
public class TransactionRequest {
    private String fromUser;
    private String toUser;
    private BigDecimal amount;
    private String description;
    private String region;
    private String transferpin;
}