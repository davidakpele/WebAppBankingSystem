package com.example.deposit.dto;

import java.util.List;
import com.example.deposit.responses.TransactionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountOverView {
    private Long Id;
    private String balance;
    private List<TransactionResponse> transactions;
}
