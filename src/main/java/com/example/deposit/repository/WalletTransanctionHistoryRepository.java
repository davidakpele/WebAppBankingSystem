package com.example.deposit.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.deposit.enums.TransactionType;
import com.example.deposit.models.Wallet;
import com.example.deposit.models.WalletTransactionHistory;

@Repository
public interface WalletTransanctionHistoryRepository extends JpaRepository<WalletTransactionHistory, Long> {

    List<WalletTransactionHistory> findByWalletId(Long walletId);

    List<WalletTransactionHistory> findByWallet(Wallet wallet);

    List<WalletTransactionHistory> findTop10ByWalletOrderByCreatedOnDesc(Wallet wallet);

    @Query("SELECT t FROM WalletTransactionHistory t WHERE t.wallet.id = :walletId AND t.wallet.userId = :userId")
    List<WalletTransactionHistory> findByWalletIdAndWalletUserId(Long walletId, Long userId);

    @Query("SELECT COUNT(nt) FROM WalletTransactionHistory nt WHERE nt.wallet.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(wth.amount), 0) FROM WalletTransactionHistory wth WHERE wth.wallet.id = :id AND wth.type = :transactionType AND wth.createdOn BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalReceived(@Param("id") Long id, @Param("transactionType") TransactionType transactionType,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(wth.amount), 0) FROM WalletTransactionHistory wth WHERE wth.wallet.id = :walletId AND wth.type = :transactionType AND wth.createdOn BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalSpent(@Param("walletId") Long walletId,
            @Param("transactionType") TransactionType transactionType, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Modifying
    @Query("DELETE FROM WalletTransactionHistory wth WHERE wth.wallet.userId IN :userIds")
    void deleteUserByIds(@Param("userIds") List<Long> userIds);

    @Query("SELECT nt FROM WalletTransactionHistory nt WHERE nt.wallet.userId = :userId")
    List<WalletTransactionHistory> findByWalletUserId(Long userId);

    @Query("SELECT wth FROM WalletTransactionHistory wth WHERE wth.wallet.userId = :userId AND wth.createdOn >= :minusMinutes ORDER BY wth.createdOn DESC")
    List<WalletTransactionHistory> findRecentTransactionsByUserId(@Param("userId") Long userId,
            @Param("minusMinutes") LocalDateTime minusMinutes);

    @Query("SELECT nt FROM WalletTransactionHistory nt WHERE nt.wallet.userId = :userId")
    List<WalletTransactionHistory> findAllByWallet_UserId(Long userId);

}
