package com.example.deposit.serviceImplementations;

import java.util.List;
import org.springframework.stereotype.Service;
import com.example.deposit.enums.CurrencyType;
import com.example.deposit.models.Wallet;
import com.example.deposit.models.WalletTransactionHistory;
import com.example.deposit.repository.WalletTransanctionHistoryRepository;
import com.example.deposit.services.WalletTransanctionHistoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalletTransanctionHistoryServiceImplementations implements WalletTransanctionHistoryService{

    private final WalletTransanctionHistoryRepository walletTransanctionHistoryRepository;
 
    @Override
    public List<WalletTransactionHistory> findByWalletIdAndUserId(Long walletId, Long userId) {
        return walletTransanctionHistoryRepository.findByWalletIdAndWalletUserId(walletId, userId);
    }

    @Override
    public List<WalletTransactionHistory> findByIdWalletId(Long walletId) {
        return walletTransanctionHistoryRepository.findByWalletId(walletId);
    }

    @Override
    @Transactional
    public List<WalletTransactionHistory> FetchUserHistoryByCurrency(Wallet wallet, CurrencyType currencyType) {
        return walletTransanctionHistoryRepository.findByWalletAndCurrency(wallet.getId(), currencyType);
    }

}
    