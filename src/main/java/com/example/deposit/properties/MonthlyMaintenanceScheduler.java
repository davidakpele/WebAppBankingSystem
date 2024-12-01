package com.example.deposit.properties;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.example.deposit.enums.DebtStatus;
import com.example.deposit.enums.TransactionType;
import com.example.deposit.models.DebtCollector;
import com.example.deposit.models.TransactionFee;
import com.example.deposit.models.Wallet;
import com.example.deposit.repository.DebtCollectorRepository;
import com.example.deposit.repository.TransactionFeeRepository;
import com.example.deposit.repository.WalletRepository;
import com.example.deposit.repository.WalletTransanctionHistoryRepository;
import java.time.LocalDate;

@Component
public class MonthlyMaintenanceScheduler {

    private final WalletTransanctionHistoryRepository transactionRepository;
    private final DebtCollectorRepository debtCollectorRepository;
    private final WalletRepository walletRepository;
    private final TransactionFeeRepository transactionFeeRepository;

    public MonthlyMaintenanceScheduler(
            DebtCollectorRepository debtCollectorRepository,
            WalletTransanctionHistoryRepository transactionRepository,
            WalletRepository walletRepository,
            TransactionFeeRepository transactionFeeRepository) {
        this.transactionRepository = transactionRepository;
        this.debtCollectorRepository = debtCollectorRepository;
        this.walletRepository = walletRepository;
        this.transactionFeeRepository = transactionFeeRepository;
    }

    @Scheduled(cron = "0 0 0 */20 * ?")
    public void calculateAndCreateDebts() {
        List<Wallet> usersWallet = walletRepository.findAll();

        for (Wallet wallet : usersWallet) {
            LocalDate startDate = LocalDate.now().minusDays(28);
            LocalDate endDate = LocalDate.now();

            // Calculate total received
            BigDecimal totalReceived = transactionRepository.calculateTotalReceived(wallet.getId(),
                    TransactionType.CREDITED, startDate, endDate);

            // Ensure totalReceived is not null, initialize to BigDecimal.ZERO if it is
            if (totalReceived == null) {
                totalReceived = BigDecimal.ZERO;
            }

            // Get maintenance Fee
            TransactionFee getFee = transactionFeeRepository.findFirstByOrderById();
            BigDecimal platformFee = BigDecimal.ZERO;

            if(getFee !=null){
                platformFee = getFee.getMakerFeePercentage();
            } else {
                platformFee = BigDecimal.valueOf(0.0055);
            }
            // Calculate maintenance fee
            BigDecimal maintenanceFee = totalReceived.multiply(platformFee);

            // Find existing debt
            Optional<DebtCollector> existingDebt = debtCollectorRepository.findByUserIdAndDebtStatus(wallet.getUserId(),
                    DebtStatus.PENDING);

            if (existingDebt.isPresent()) {
                DebtCollector debt = existingDebt.get();
                debt.setDueAmount(debt.getDueAmount().add(maintenanceFee));

                // Check if the wallet balance is less than the due amount
                if (wallet.getBalance().compareTo(debt.getDueAmount()) < 0) {
                    debt.setDebtStatus(DebtStatus.OVERDUE); // Set status to OVERDUE if balance is insufficient
                } else {
                    debt.setDebtStatus(DebtStatus.PENDING); // Keep it PENDING if the balance is sufficient
                }

                debtCollectorRepository.save(debt);
            } else {
                // Create new debt if none exists
                DebtCollector debt = new DebtCollector();
                debt.setUserId(wallet.getUserId());
                debt.setAmount(maintenanceFee);
                debt.setDueAmount(BigDecimal.ZERO);
                debt.setDebtStatus(DebtStatus.PENDING); // Initially set to PENDING
                debt.setDescription("Monthly maintenance fee");
                debtCollectorRepository.save(debt);
            }
        }
    }

}
