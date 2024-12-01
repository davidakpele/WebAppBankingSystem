package com.example.deposit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CoinWalletDTO {
    private Long id;
    private BigDecimal balance;
    @JsonProperty("crypto_id")
    private String cryptoId;
    private Long userId;
    @JsonProperty("wallet_address")
    private String walletAddress;
}
