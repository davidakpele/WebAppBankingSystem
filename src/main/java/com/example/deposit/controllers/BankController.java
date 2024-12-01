package com.example.deposit.controllers;

import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.deposit.responses.Error;
import com.example.deposit.serviceImplementations.UserServiceClient;
import com.example.deposit.services.BankListService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import com.example.deposit.dto.PayStackBankList;
import com.example.deposit.models.BankList;
import com.example.deposit.payloads.BankAccountRequest;
import com.example.deposit.properties.TokenExtractor;
import com.example.deposit.repository.BankListRepository;

@RestController
@RequestMapping("/api/v1/bank")
@RequiredArgsConstructor
public class BankController {

    private final BankListService bankListService;
    private final BankListRepository bankListRepository;
    private final UserServiceClient userServiceClient;
    private final TokenExtractor tokenExtractor;
    private final HttpServletRequest httpServletRequest;
     
    @PostMapping("/add-new-bank")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> addBankAccount(@RequestBody BankAccountRequest request, Authentication authentication) {
        try {
            String token = tokenExtractor.extractToken(httpServletRequest);
            if (token.isBlank() || token.isEmpty()) {
                return Error.createResponse("UNAUTHORIZED*.",
                        HttpStatus.UNAUTHORIZED, "Require token to access this endpoint, Missing valid token.");
            }
            if (request.getAccountNumber() == null || request.getAccountNumber().isEmpty()) {
                return Error.createResponse("Account number require*.",
                        HttpStatus.BAD_REQUEST, "Account number can not be empty");
            }
            if (request.getAccount_holder_name() == null || request.getAccount_holder_name().isEmpty()) {
                return Error.createResponse("Account Holder Name require*.",
                        HttpStatus.BAD_REQUEST, "Name who's this very account belong to requires*.");
            }
            if (request.getBankCode() == null || request.getBankCode().isEmpty()) {
                return Error.createResponse("Bank code requires*.",
                        HttpStatus.BAD_REQUEST, "The Cdde of the bank you selected requires*.");
            }
            if (request.getBankName() == null || request.getBankName().isEmpty()) {
                return Error.createResponse("Name of the bank selected requires*.",
                        HttpStatus.BAD_REQUEST, "Bank is can not be empty*.");
            } else if (existAccount(request.getAccountNumber())) {
                return Error.createResponse("Sorry..! Account details already added to you profile*",
                        HttpStatus.BAD_REQUEST, "Account details already exists");
            }
            Optional<String> response = bankListService.addNewBankDetails(request, token, authentication);
            if (response.isPresent()) {
                return ResponseEntity.status(500)
                        .body("Error verifying creating storing your informations in the system.");
            } else {
                return Error.createResponse("Successfully Added*.",
                        HttpStatus.CREATED, "Bank account successfully added to you profile*.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error verifying bank account: " + e.getMessage());
        }
    }

    @GetMapping("/verify-user-bank-details")
    public ResponseEntity<?> verifyBankAccount(@RequestParam("accountNumber") String accountNumber, @RequestParam("bankCode") String bankCode) {
        try {
        
            Optional<BankList> bankDetails = bankListRepository.findByAccountNumberAndBankCode(accountNumber, bankCode);
            if (bankDetails.isPresent()) {
                return Error.createResponse("This account already been assigned to another user.", HttpStatus.BAD_REQUEST,
                        "Already been assigned to another user.");
            }
            Optional<String> response = bankListService.verifyBankAccount(accountNumber, bankCode);
            if (response.isPresent()) {
                return ResponseEntity.ok(response.get());
            } else {
                return Error.createResponse("Error verifying bank account: No response", HttpStatus.NOT_FOUND,
                        "Error verifying bank account: No response");
            }
        } catch (Exception e) {
            return Error.createResponse("Error verifying bank account: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error verifying bank account: " + e.getMessage());
        }
    }
    
    @GetMapping("/paystack/get-bank-list")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getUserBankList(Authentication authentication) {
        String token = tokenExtractor.extractToken(httpServletRequest);
        if (token.isBlank() || token.isEmpty()) {
            return Error.createResponse("UNAUTHORIZED*.",
                    HttpStatus.UNAUTHORIZED, "Require token to access this endpoint, Missing valid token.");
        }
        String username = authentication.getName();
        var userInfo = userServiceClient.getUserByUsername(username, token);
        
        List<BankList> bankList = bankListService.getBankListByUserId(userInfo.getId());

        if (bankList.isEmpty()) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.ok(bankList);
        }
    }

    @GetMapping("/bank-list")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getPayStackBankList(Authentication authentication) {
        String username = authentication.getName();
        String token = tokenExtractor.extractToken(httpServletRequest);
        if (token.isBlank() || token.isEmpty()) {
            return Error.createResponse("UNAUTHORIZED*.",
                    HttpStatus.UNAUTHORIZED, "Require token to access this endpoint, Missing valid token.");
        }
        var userInfo = userServiceClient.getUserByUsername(username, token);
        if (userInfo.getUsername().equals(username)) {
            Optional<List<PayStackBankList>> banks = bankListService.fetchAllBanks();
            if (banks.isPresent()) {
                return ResponseEntity.ok(banks.get());
            } else { 
                return ResponseEntity.status(500).body("Error fetching bank list");
            }
        }
        return Error.createResponse("FORBIDDEN", HttpStatus.FORBIDDEN,
                "Denied Access");
    }

    private boolean existAccount(String accountNumber) {
        Optional<BankList> existingUsers = bankListRepository.findByAccountNumber(accountNumber);
        return existingUsers.isPresent();
    }



}
