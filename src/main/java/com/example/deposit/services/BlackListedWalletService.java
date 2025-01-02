package com.example.deposit.services;

import java.util.concurrent.CompletableFuture;
import org.springframework.http.ResponseEntity;
import com.example.deposit.enums.BannedReasons;

public interface BlackListedWalletService {
    CompletableFuture<ResponseEntity<?>> addUserToBlackList(Long userWalletId, BannedReasons reasons);

    CompletableFuture<ResponseEntity<?>> updateUserBlackList(Long userWalletId, Boolean isBlock);

    CompletableFuture<ResponseEntity<?>> deletedUserFromBlackList(Long userWalletId);
}
