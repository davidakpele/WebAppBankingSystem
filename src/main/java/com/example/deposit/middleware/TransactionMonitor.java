package com.example.deposit.middleware;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import com.example.deposit.dto.UserDTO;
import com.example.deposit.enums.TransactionType;
import com.example.deposit.models.WalletTransactionHistory;
import com.example.deposit.payloads.TransactionRequest;
import com.example.deposit.repository.BlackListedWalletRepository;
import com.example.deposit.repository.WalletTransanctionHistoryRepository;
import com.example.deposit.serviceImplementations.UserServiceClient;

@Component
public class TransactionMonitor {

    private final WalletTransanctionHistoryRepository walletTransanctionHistoryRepository;
    private final UserServiceClient userServiceClient;
    private final BlackListedWalletRepository blackListedWalletRepository;

    public TransactionMonitor(BlackListedWalletRepository blackListedWalletRepository, 
            UserServiceClient userServiceClient,
            WalletTransanctionHistoryRepository walletTransanctionHistoryRepository) {
        this.walletTransanctionHistoryRepository = walletTransanctionHistoryRepository;
        this.userServiceClient = userServiceClient;
        this.blackListedWalletRepository = blackListedWalletRepository;
        
    }

    // High Volume or Multiple Transactions in Short Timeframe
    public boolean isHighVolumeOrFrequentTransactions(Long userId) {
        List<WalletTransactionHistory> recentTransactions = walletTransanctionHistoryRepository.findRecentTransactionsByUserId(userId, LocalDateTime.now().minusMinutes(10));
        BigDecimal totalAmount = recentTransactions.stream()
                .map(WalletTransactionHistory::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Flag if more than 5 transactions in 10 minutes or total amount exceeds threshold
        return recentTransactions.size() > 5 || totalAmount.compareTo(new BigDecimal("2500000.00")) > 0;
    }

    // Inconsistent Behavior
    public boolean isInconsistentBehavior(Long userId) {
        // Fetch recent transactions (e.g., last 24 hours)
        LocalDateTime minus24Hours = LocalDateTime.now().minusHours(24);
        List<WalletTransactionHistory> recentTransactions = walletTransanctionHistoryRepository.findRecentTransactionsByUserId(userId, minus24Hours);

        if (recentTransactions.isEmpty()) {
            // If no recent transactions, behavior can't be inconsistent
            return false;
        }

        // Criteria 1: Check for sudden large transactions compared to the user's typical behavior
        BigDecimal averageAmount = calculateAverageTransactionAmount(userId);
        for (WalletTransactionHistory transaction : recentTransactions) {
            // threshold: if a transaction is more than 5 times the average amount
            if (transaction.getAmount().compareTo(averageAmount.multiply(BigDecimal.valueOf(5))) > 0) {
                return true;
            }
        }

        // Criteria 2: Check if there are too many transactions in a short period (e.g., > 10 in 24 hours)
        if (recentTransactions.size() > 10) {
            return true;
        }

        // Criteria 3: Check for transactions from different IP addresses (suggesting multiple locations or devices)
        String lastIpAddress = recentTransactions.get(0).getIpAddress();
        for (WalletTransactionHistory transaction : recentTransactions) {
            if (!transaction.getIpAddress().equals(lastIpAddress)) {
                return true;
            }
        }

        // Criteria 4: Check for unusual transaction types (e.g., frequent withdrawals)
        long withdrawalCount = recentTransactions.stream()
                .filter(t -> t.getType() == TransactionType.WITHDRAW)
                .count();
        if (withdrawalCount > 5) { 
            return true;
        }

        // If no inconsistencies detected
        return false;
    }

    // Transactions Involving High-Risk Regions
    public boolean isHighRiskRegion(TransactionRequest request) {
        String region = request.getRegion(); 
        List<String> highRiskRegions = Arrays.asList("Philippines", "Venezuela", "Vietnam", "Yemen", "Haiti"); 
        return highRiskRegions.contains(region);
    }

   
    // Transactions Associated with Newly Created or Unverified Wallets
    public boolean isUnverifiedOrNewWallet(Long userId, String token) {
        UserDTO user = userServiceClient.getUserById(userId, token);
        if (user == null)
            return false;
        /**
         * Assume verificationStatus is a boolean indicating if the wallet is verified
         *  This will now return true if the user was created within the last 3 Minutes or if the user is not enabled.
         *  */ 
        return !user.isEnabled() || user.getCreatedOn().isAfter(LocalDateTime.now().minusMinutes(3));
    }

    //  Deposits Followed by Immediate Transfers
    public boolean isImmediateTransferAfterDeposit(Long userId) {
        List<WalletTransactionHistory> recentTransactions = walletTransanctionHistoryRepository.findRecentTransactionsByUserId(userId, LocalDateTime.now().minusHours(1));

        boolean hasDeposit = recentTransactions.stream()
                .anyMatch(tx -> tx.getType() == TransactionType.DEPOSIT);

        boolean hasTransfer = recentTransactions.stream()
                .anyMatch(tx -> tx.getType() == TransactionType.DEBITED);

        // Return true if deposit is followed immediately by transfer
        return hasDeposit && hasTransfer;
    }

    // Transactions From Blacklisted Wallet Addresses
    public boolean isFromBlacklistedAddress(Long walletId) {
        Boolean exists = blackListedWalletRepository.findByWalletId(walletId);
        return exists != null && exists;
    }

    // Helper method to calculate the average transaction amount for a user
    @SuppressWarnings("deprecation")
    private BigDecimal calculateAverageTransactionAmount(Long userId) {
        List<WalletTransactionHistory> allTransactions = walletTransanctionHistoryRepository
                .findAllByWallet_UserId(userId);

        // Handle the case where there are no transactions
        if (allTransactions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Safely map and handle null amounts
        BigDecimal totalAmount = allTransactions.stream()
                .map(transaction -> transaction.getAmount() != null ? transaction.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Return the average transaction amount
        return totalAmount.divide(BigDecimal.valueOf(allTransactions.size()), BigDecimal.ROUND_HALF_UP);
    }

}
