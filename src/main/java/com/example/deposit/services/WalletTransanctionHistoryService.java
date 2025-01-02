package com.example.deposit.services;

import java.util.List;
import com.example.deposit.enums.CurrencyType;
import com.example.deposit.models.Wallet;
import com.example.deposit.models.WalletTransactionHistory;

public interface WalletTransanctionHistoryService {

    List<WalletTransactionHistory> findByIdWalletId(Long walletId);

    List<WalletTransactionHistory> findByWalletIdAndUserId(Long walletId, Long id);

    List<WalletTransactionHistory> FetchUserHistoryByCurrency(Wallet wallet, CurrencyType currencyType);

}
