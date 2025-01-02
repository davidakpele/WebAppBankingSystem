package com.example.deposit.services;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.ResponseEntity;
import com.example.deposit.models.BeneficiaryDairy;
import com.example.deposit.payloads.BankAccountRequest;

public interface BeneficiaryDairyService {
    List<BeneficiaryDairy> AllUserBeneficiary(Long userId);

    CompletableFuture<ResponseEntity<?>> deleteUserBeneficiaryById(Long Id);

    CompletableFuture<ResponseEntity<?>> addUserBeneficiary(BankAccountRequest request, Long userId);

    CompletableFuture<ResponseEntity<?>> deleteBeneficiaryByIds(List<Long> ids);
} 
