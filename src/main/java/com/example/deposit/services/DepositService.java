package com.example.deposit.services;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import com.example.deposit.payloads.DepositRequest;

public interface DepositService {
    ResponseEntity<?> createDeposit(DepositRequest request, String token, Authentication authentication);
}
