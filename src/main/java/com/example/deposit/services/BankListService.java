package com.example.deposit.services;

import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import com.example.deposit.dto.PayStackBankList;
import com.example.deposit.models.BankList;
import com.example.deposit.payloads.BankAccountRequest;

public interface BankListService {

    Optional<String> verifyBankAccount(String accountNumber, String bankCode);

    Optional<String> addNewBankDetails(BankAccountRequest request, String token, Authentication authentication);

    List<BankList> getBankListByUserId(Long id);

    Optional<List<PayStackBankList>> fetchAllBanks();

    Optional<BankList> findUserBankByBankId(Long bankId, Long userId);
    
}
