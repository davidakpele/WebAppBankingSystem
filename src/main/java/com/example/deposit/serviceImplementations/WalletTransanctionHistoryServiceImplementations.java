package com.example.deposit.serviceImplementations;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.example.deposit.dto.UserDTO;
import com.example.deposit.models.Wallet;
import com.example.deposit.models.WalletTransactionHistory;
import com.example.deposit.repository.WalletRepository;
import com.example.deposit.repository.WalletTransanctionHistoryRepository;
import com.example.deposit.services.WalletTransanctionHistoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalletTransanctionHistoryServiceImplementations implements WalletTransanctionHistoryService{

    private final WalletTransanctionHistoryRepository walletTransanctionHistoryRepository;
    private final WalletRepository walletRepository;
 
    @Override
    @Transactional
    public List<WalletTransactionHistory> getWalletTransactionsHistoryByWallet(Wallet ewallet) {
        return walletTransanctionHistoryRepository.findByWallet(ewallet);
    }

    @Override
    public List<WalletTransactionHistory> findByWalletIdAndUserId(Long walletId, Long userId) {
        return walletTransanctionHistoryRepository.findByWalletIdAndWalletUserId(walletId, userId);
    }

    @Override
    public Optional<Wallet> getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId);
    }

    @Override
    public Wallet getWalletByUser(UserDTO user) {
        return walletRepository.findByUserId(user.getId()).orElse(null);
    }

    // @Override
    // public Optional<UserDTO> findUserById(Long id) {
    //     return Optional.ofNullable(userServiceClient.getUserById(id));
    // }

    @Override
    public Wallet getUserWalletByUserWalletId(Long userId) {
        return walletRepository.findByUserId(userId).orElse(null);
    }

    @Override
    public List<WalletTransactionHistory> findByIdWalletId(Long walletId) {
        return walletTransanctionHistoryRepository.findByWalletId(walletId);
    }

}
    