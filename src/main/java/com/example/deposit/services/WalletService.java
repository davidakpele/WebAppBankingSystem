package com.example.deposit.services;

import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import com.example.deposit.dto.UserDTO;
import com.example.deposit.enums.CurrencyType;
import com.example.deposit.models.Wallet;
import com.example.deposit.payloads.ExternalTransactionRequest;
import com.example.deposit.payloads.TransactionRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface WalletService {
    Wallet getWalletByUser(UserDTO user, String token);

    Optional<Wallet> getWalletByUserId(Long userId, String token);

    ResponseEntity<?> createTransferPin(TransactionRequest request, String token, Authentication authentication);

    ResponseEntity<?> getBalance(String username, String token);

    ResponseEntity<?> getBalanceByCurrencyType(Long userId, CurrencyType currency, String token);
    Wallet getUserWalletByUserWalletId(Long userId);

    ResponseEntity<?> TransferWithUsernameToUserInSamePlatform(String username, TransactionRequest request,
            Authentication authentication, String token, HttpServletRequest httpRequest);

    ResponseEntity<?> transferToExternalUserOutSidePlatform(String username, ExternalTransactionRequest request,
            Authentication authentication, String token, HttpServletRequest httpReques);

    ResponseEntity<?> creditWalletFromCryptoTrade(TransactionRequest request, String username, String token);        
}