package com.example.deposit.controllers;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import com.example.deposit.dto.AccountOverView;
import com.example.deposit.dto.UserDTO;
import com.example.deposit.models.Wallet;
import com.example.deposit.models.WalletTransactionHistory;
import com.example.deposit.payloads.TransactionRequest;
import com.example.deposit.properties.TokenExtractor;
import com.example.deposit.services.WalletService;
import com.example.deposit.services.WalletTransanctionHistoryService;
import com.example.deposit.utils.Refactory;
import lombok.RequiredArgsConstructor;
import com.example.deposit.responses.Error;
import com.example.deposit.responses.TransactionResponse;
import com.example.deposit.serviceImplementations.UserServiceClient;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final UserServiceClient userServiceClient;
    private final WalletTransanctionHistoryService walletTransanctionHistoryService; 
    private final TokenExtractor tokenExtractor;
    private final HttpServletRequest httpServletRequest;

    @PostMapping("/set/pin")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> userSetTransferPin(@RequestBody TransactionRequest request,
            Authentication authentication) {
        String providedPin = request.getTransferpin();
        String token = tokenExtractor.extractToken(httpServletRequest);
        if (token.isBlank() || token.isEmpty()) {
            return Error.createResponse("UNAUTHORIZED*.",
                    HttpStatus.UNAUTHORIZED, "Require token to access this endpoint, Missing valid token.");
        }        
        if (providedPin == null || providedPin.isEmpty()) {
            return Error.createResponse("Transfer pin is required.", HttpStatus.BAD_REQUEST,
                    "Please provide your transfer pin.");
        }
        return walletService.createTransferPin(request, token, authentication);
    }

    // view all user transactions history
    @GetMapping("/history/overview")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> getAccountOverview(Authentication authentication) {
        String username = authentication.getName();
        String token = tokenExtractor.extractToken(httpServletRequest);
        if (token.isBlank() || token.isEmpty()) {
            return Error.createResponse("UNAUTHORIZED*.",
                    HttpStatus.UNAUTHORIZED, "Require token to access this endpoint, Missing valid token.");
        }
        // Check if user exists
        UserDTO user = userServiceClient.getUserByUsername(username, token);
        if (user == null) {
            return Error.createResponse("User not found", HttpStatus.BAD_REQUEST,
                    "User does not exists in our sysetm.");
        }

        // Check if wallet exists
        Wallet ewallet = walletService.getWalletByUser(user, token);
        
        if (ewallet == null) {
            return Error.createResponse("Wallet not found", HttpStatus.BAD_REQUEST,
                    "We couldn't find wallet registered under this user.");
        }
 
        // Check if transactions exist
        List<WalletTransactionHistory> transactions = walletTransanctionHistoryService.getWalletTransactionsHistoryByWallet(ewallet);
        if (transactions.isEmpty()) {
            return Error.createResponse("No transactions history found", HttpStatus.BAD_REQUEST,
                    "We couldn't find any transaction history this user.");
        }

        // Convert transactions to TransactionResponse
        List<TransactionResponse> transactionResponses = transactions.stream()
            .map(transaction -> {
                TransactionResponse response = new TransactionResponse();
                response.setId(transaction.getId());
                response.setWalletId(transaction.getWallet().getId());
                response.setSessionId(transaction.getSessionId());
                response.setAmount(Refactory.formatBigDecimal(transaction.getAmount()));
                response.setType(transaction.getType());
                response.setDescription(transaction.getDescription());
                response.setMessage(transaction.getMessage());
                response.setStatus(transaction.getStatus());
                response.setCreatedOn(transaction.getCreatedOn());
                
                return response;
            }).collect(Collectors.toList());

        String formattedBalance = Refactory.formatBigDecimal(ewallet.getBalance());
        
        AccountOverView accountOverview = new AccountOverView();
        accountOverview.setId(user.getId());
        accountOverview.setBalance(formattedBalance);
        accountOverview.setTransactions(transactionResponses);
        return ResponseEntity.ok(accountOverview);
    }

    @GetMapping("/balance")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getBalance(Authentication authentication) {
        String username = authentication.getName();
        String token = tokenExtractor.extractToken(httpServletRequest);
        if (token.isBlank() || token.isEmpty()) {
            return Error.createResponse("UNAUTHORIZED*.",
                    HttpStatus.UNAUTHORIZED, "Require token to access this endpoint, Missing valid token.");
        }
        return walletService.getBalance(username, token);
    }

    
}
