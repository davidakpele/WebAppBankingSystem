package com.example.deposit.controllers;

import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.deposit.responses.Error;
import com.example.deposit.services.WalletService;
import com.example.deposit.payloads.ExternalTransactionRequest;
import com.example.deposit.payloads.TransactionRequest;
import com.example.deposit.properties.TokenExtractor;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/transfer")
public class TransactionController {

    private final WalletService walletService;
    private final TokenExtractor tokenExtractor;
    private final HttpServletRequest httpServletRequest;
    
    public TransactionController(WalletService walletService, TokenExtractor tokenExtractor,
            HttpServletRequest httpServletRequest) {
        this.walletService = walletService;
        this.tokenExtractor = tokenExtractor;
        this.httpServletRequest= httpServletRequest;
    }

    @PostMapping("/platform/user/send")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> TransferWithUsernameToUserInSamePlatform(@RequestBody TransactionRequest request, Authentication authentication, HttpServletRequest httpRequest) {
        String username = authentication.getName();
        String token = tokenExtractor.extractToken(httpServletRequest);
        if (token.isBlank() || token.isEmpty()) {
            return Error.createResponse("UNAUTHORIZED*.",
                    HttpStatus.UNAUTHORIZED, "Require token to access this endpoint, Missing valid token.");
        }

        if (request.getFromUser().isEmpty()) {
            return Error.createResponse("Sender id is require.", HttpStatus.BAD_REQUEST,
                    "Please provide the sender user id.");
        }

        if (request.getToUser().isEmpty()) {
            return Error.createResponse("Recipient id is require.", HttpStatus.BAD_REQUEST,
                    "Please provide the Recipient user id.");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Error.createResponse("Amount is required and must be greater than zero.", HttpStatus.BAD_REQUEST,
                    "Please provide a valid amount you want to transfer to this user.");
        }

        String providedPin = request.getTransferpin();

        if (providedPin == null || providedPin.isEmpty()) {
            return Error.createResponse("Transfer pin is required.", HttpStatus.BAD_REQUEST,
                    "Please provide your transfer pin.");
        }
        return walletService.TransferWithUsernameToUserInSamePlatform(username, request, authentication, token, httpRequest);
    }
    
    @PostMapping("/external/user/send")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> transferToExternalUserOutSidePlatform(@RequestBody ExternalTransactionRequest request, Authentication authentication, HttpServletRequest httpRequest) {
        String username = authentication.getName();
        String token = tokenExtractor.extractToken(httpServletRequest); 
        if (token.isBlank() || token.isEmpty()) {
            return Error.createResponse("UNAUTHORIZED*.",
                    HttpStatus.UNAUTHORIZED, "Require token to access this endpoint, Missing valid token.");
        }

        if (request.getAmount() == null || request.getAmount().equals("")) {
            return Error.createResponse("Amount is require.*", HttpStatus.BAD_REQUEST,
                    "Please provide the amount you want to transfer to this user.");
        }

        String providedPin = request.getTransferpin();

        if (providedPin == null || providedPin.isEmpty()) {
            return Error.createResponse("Transfer pin is required.", HttpStatus.BAD_REQUEST,
                    "Please provide your transfer pin.");
        }

        return walletService.transferToExternalUserOutSidePlatform(username, request, authentication, token, httpRequest);
    }


}
