package com.example.deposit.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import com.example.deposit.enums.TransactionType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponse {
    private Long id;
    private Long walletId;
    private String amount;
    private TransactionType type;
    private String description;
    private String message;
    private String sessionId;
    private String status;
    private LocalDateTime createdOn;
}