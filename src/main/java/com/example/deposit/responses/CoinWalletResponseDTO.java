package com.example.deposit.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import com.example.deposit.dto.CoinWalletDTO;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CoinWalletResponseDTO {
    private List<CoinWalletDTO> coinWallets;
}
