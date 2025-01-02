package com.example.deposit.serviceImplementations;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.example.deposit.models.BeneficiaryDairy;
import com.example.deposit.payloads.BankAccountRequest;
import com.example.deposit.repository.BeneficiaryDairyRepository;
import com.example.deposit.services.BeneficiaryDairyService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BeneficiaryDairyServiceImplementations implements BeneficiaryDairyService {

    private final BeneficiaryDairyRepository beneficiaryDairyRepository;

    @Transactional
    public List<BeneficiaryDairy> AllUserBeneficiary(Long userId) {
        return beneficiaryDairyRepository.findAllUserBeneficiaryByUserId(userId);
    }

    @Async
    @Transactional
    public CompletableFuture<ResponseEntity<?>> deleteUserBeneficiaryById(Long Id) {
        Optional<BeneficiaryDairy> getBeneficiaryById = Optional.ofNullable(beneficiaryDairyRepository.findById(Id)
                .orElseThrow(() -> new RuntimeException("Beneficiary Not Found.")));
        if (getBeneficiaryById.isPresent()) {
            beneficiaryDairyRepository.deleteById(Id);
            return CompletableFuture.completedFuture(ResponseEntity.noContent().build());
        }
        return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
    }

    @Async
    @Transactional
    public CompletableFuture<ResponseEntity<?>> addUserBeneficiary(BankAccountRequest request, Long userId) {
        BeneficiaryDairy addNew = BeneficiaryDairy.builder()
            .accountHolderName(request.getAccount_holder_name())
            .accountNumber(request.getAccountNumber())
            .bankName(request.getBankName())
            .bankCode(request.getBankCode())
            .userId(userId)
        .build();
        beneficiaryDairyRepository.save(addNew);
        return CompletableFuture.completedFuture(ResponseEntity.accepted().build());
    }

    @Async
    @Transactional
     public CompletableFuture<ResponseEntity<?>> deleteBeneficiaryByIds(List<Long> ids) {
        beneficiaryDairyRepository.deleteUserBeneficiaryByIds(ids);
        return CompletableFuture.completedFuture(ResponseEntity.noContent().build());
    }
}
