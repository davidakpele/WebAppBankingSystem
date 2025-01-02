package com.example.deposit.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.deposit.models.BankList;

@Repository
public interface BankListRepository extends JpaRepository<BankList, Long> {

    @Query("select b from BankList b where b.accountNumber =:accountNumber")
    Optional<BankList> findByAccountNumber(String accountNumber);

    @Query("select b from BankList b where b.userId =:id")
    List<BankList> findByUserId(Long id);

    @Query("select b from BankList b where b.accountNumber =:accountNumber and b.bankCode=:bankCode")
    Optional<BankList> findByAccountNumberAndBankCode(String accountNumber, String bankCode);

    @Query("select b from BankList b where b.id=:bankId and b.userId =:userId")
    Optional<BankList> findByIdAndUserId(Long bankId, Long userId);

    @Modifying
    @Query("DELETE FROM BankList b WHERE b.userId IN :ids")
    void deleteUserBankDetailsByIds(List<Long> ids);
}