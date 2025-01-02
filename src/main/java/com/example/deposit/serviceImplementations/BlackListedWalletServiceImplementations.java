package com.example.deposit.serviceImplementations;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.example.deposit.enums.BannedReasons;
import com.example.deposit.models.BlackListedWallet;
import com.example.deposit.repository.BlackListedWalletRepository;
import com.example.deposit.services.BlackListedWalletService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BlackListedWalletServiceImplementations implements BlackListedWalletService{

    private final BlackListedWalletRepository blackListedWalletRepository;
 
    @Async
    public CompletableFuture<ResponseEntity<?>> addUserToBlackList(Long userWalletId, BannedReasons reasons) {
        BlackListedWallet blackListedWallet =  BlackListedWallet.builder()
                .walletId(userWalletId)
                .bankBannedReasons(reasons)
                .isBlock(true)
                .build();
        blackListedWalletRepository.save(blackListedWallet);
        return CompletableFuture.completedFuture(ResponseEntity.accepted().build());
    }

    @Async
    @Transactional
    public CompletableFuture<ResponseEntity<?>> updateUserBlackList(Long userWalletId, Boolean isBlock) {
        Optional<BlackListedWallet> blacklistedUserOptional = blackListedWalletRepository
                .findByUserWalletId(userWalletId);
        if (blacklistedUserOptional.isPresent()) {
            BlackListedWallet upListedWallet = blacklistedUserOptional.get();
            upListedWallet.setIsBlock(isBlock);
            blackListedWalletRepository.save(upListedWallet);
            return CompletableFuture.completedFuture(ResponseEntity.accepted().build());
        }
        return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
    }

    @Async
    @Transactional
    public CompletableFuture<ResponseEntity<?>> deletedUserFromBlackList(Long userWalletId) {
        Optional<BlackListedWallet> blacklistedUserOptional = blackListedWalletRepository.findByUserWalletId(userWalletId);
        if (blacklistedUserOptional.isPresent()) {
            blackListedWalletRepository.deleteByWalletId(userWalletId);
            return CompletableFuture.completedFuture(ResponseEntity.noContent().build());
        }
        return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
    }
}
