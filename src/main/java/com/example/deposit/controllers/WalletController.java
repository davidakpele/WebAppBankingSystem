package com.example.deposit.controllers;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.deposit.dto.AccountOverView;
import com.example.deposit.dto.UserDTO;
import com.example.deposit.enums.CurrencyType;
import com.example.deposit.models.Wallet;
import com.example.deposit.models.WalletTransactionHistory;
import com.example.deposit.payloads.TransactionRequest;
import com.example.deposit.properties.TokenExtractor;
import com.example.deposit.responses.Error;
import com.example.deposit.responses.TransactionResponse;
import com.example.deposit.serviceImplementations.UserServiceClient;
import com.example.deposit.services.WalletService;
import com.example.deposit.services.WalletTransanctionHistoryService;
import com.example.deposit.utils.Refactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/wallet")
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

    // view all user transactions history on a specific wallet
    @GetMapping("/history/overview/currency/{currency}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> getAccountOverview(@PathVariable String currency, Authentication authentication) {
        String username = authentication.getName();
        String token = tokenExtractor.extractToken(httpServletRequest);
        if (token.isBlank() || token.isEmpty()) {
            return Error.createResponse("UNAUTHORIZED*.",
                    HttpStatus.UNAUTHORIZED, "Require token to access this endpoint, Missing valid token.");
        }
        if (currency.isBlank() || currency.isEmpty()) {
            return Error.createResponse("Currency Type is require*.",
                    HttpStatus.BAD_REQUEST, "Please provide paramenter Currency type.");
        }
        // Check if user exists
        UserDTO user = userServiceClient.getUserByUsername(username, token);
        if (user == null) {
            return Error.createResponse("User not found", HttpStatus.BAD_REQUEST,
                    "User does not exists in our sysetm.");
        }

        // Check if wallet exists
        Wallet wallet = walletService.getWalletByUser(user, token);
        
        if (wallet == null) {
            return Error.createResponse("Wallet not found", HttpStatus.BAD_REQUEST,
                    "We couldn't find wallet registered under this user.");
        }
        if (!Arrays.stream(CurrencyType.values()).anyMatch(ct -> ct.name().equals(currency.toString().toUpperCase()))) {
            return Error.createResponse("Invalid Currency provided.*.",
                    HttpStatus.BAD_REQUEST,
                    "Please provide Currency type. any of this list (USD, EUR, NGN,GBP, JPY,AUD,CAD, CHF, CNY, OR INR)");
        }
        CurrencyType currencyType = CurrencyType.valueOf(currency.toString().toUpperCase());

        // Check if transactions exist
        List<WalletTransactionHistory> transactions = walletTransanctionHistoryService.FetchUserHistoryByCurrency(wallet, currencyType);
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

        String naira_bal = Refactory.formatBigDecimal(wallet.getBalance().get(currencyType.name()));
        AccountOverView accountOverview = new AccountOverView();
        accountOverview.setId(user.getId());
        accountOverview.setBalance(naira_bal);
        accountOverview.setTransactions(transactionResponses);
        return ResponseEntity.ok(accountOverview);
    }

    
    @GetMapping("/all/assets/balance")
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

    @GetMapping("/balance/userId/{userId}/currency/{currency}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getBalanceByCurrencyType(@PathVariable String currency, @PathVariable Long userId) {
        String token = tokenExtractor.extractToken(httpServletRequest);
        if (token.isBlank() || token.isEmpty()) {
            return Error.createResponse("UNAUTHORIZED*.",
                    HttpStatus.UNAUTHORIZED, "Require token to access this endpoint, Missing valid token.");
        }

        if (currency.isBlank() || currency.isEmpty()) {
            return Error.createResponse("Currency Type is require*.",
                    HttpStatus.BAD_REQUEST, "Please provide paramenter Currency type.");
        }
        if (userId ==null) {
            return Error.createResponse("User Id is require*.",
                    HttpStatus.BAD_REQUEST, "Please provide paramenter User id.");
        }

        if (!Arrays.stream(CurrencyType.values()).anyMatch(ct -> ct.name().equals(currency.toString().toUpperCase()))) {
            return Error.createResponse("Invalid Currency provided.*.",
                    HttpStatus.BAD_REQUEST,
                    "Please provide Currency type. any of this list (USD, EUR, NGN,GBP, JPY,AUD,CAD, CHF, CNY, OR INR)");
        }
        CurrencyType currencyType = CurrencyType.valueOf(currency.toString().toUpperCase());
        return walletService.getBalanceByCurrencyType(userId, currencyType, token);
    }
    
    @PostMapping("/credit/account")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> creditWalletFromCryptoTrade(@RequestBody TransactionRequest request, Authentication authentication) {
        String token = tokenExtractor.extractToken(httpServletRequest);
        String username = authentication.getName();
        if (token.isBlank() || token.isEmpty()) {
            return Error.createResponse("UNAUTHORIZED*.",
                    HttpStatus.UNAUTHORIZED, "Require token to access this endpoint, Missing valid token.");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Error.createResponse("Amount is require and must be greater than zero.", HttpStatus.BAD_REQUEST,
                    "Please provide a valid amount.");
        }

        if (request.getRecipientUserId() == null) {
            return Error.createResponse("Recipient Id require", HttpStatus.BAD_REQUEST,
                    "Please provide the seller Id.");
        }

        if (request.getCreditorUserId() == null) {
            return Error.createResponse("Buyer Id require", HttpStatus.BAD_REQUEST,
                    "Please provide the buyer Id.");
        }
        
        if (!Arrays.stream(CurrencyType.values()).anyMatch(ct -> ct.name().equals(request.getCurrencyType().toString().toUpperCase()))) {
            return Error.createResponse("Invalid Currency provided.*.",
                    HttpStatus.BAD_REQUEST,
                    "Please provide Currency type. any of this list (USD, EUR, NGN,GBP, JPY,AUD,CAD, CHF, CNY, OR INR)");
        }
       
        return walletService.creditWalletFromCryptoTrade(request, username, token);
    }

}
