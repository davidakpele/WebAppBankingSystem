package com.example.deposit.services;

import java.util.List;
import java.util.Optional;
import com.example.deposit.dto.UserDTO;
import com.example.deposit.models.Wallet;
import com.example.deposit.models.WalletTransactionHistory;

public interface WalletTransanctionHistoryService {

    List<WalletTransactionHistory> findByIdWalletId(Long walletId);

    List<WalletTransactionHistory> getWalletTransactionsHistoryByWallet(Wallet ewallet);

    List<WalletTransactionHistory> findByWalletIdAndUserId(Long walletId, Long id);

    Optional<Wallet> getWalletByUserId(Long userId);

    Wallet getWalletByUser(UserDTO user);

    Wallet getUserWalletByUserWalletId(Long userId);

    // Optional<UserDTO> findUserById(Long id);

}
