package com.example.deposit.properties;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.deposit.enums.CurrencyType;
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
        List<Wallet> usersWallets = walletRepository.findAll();

        for (Wallet wallet : usersWallets) {
            LocalDate startDate = LocalDate.now().minusDays(28);
            LocalDate endDate = LocalDate.now();

            for (Map.Entry<String, BigDecimal> entry : wallet.getBalance().entrySet()) {
                String currencyType = entry.getKey();
                BigDecimal walletBalance = entry.getValue();

                CurrencyType enumCurrencyType;
                enumCurrencyType = CurrencyType.fromString(currencyType);
                
                // Calculate total received for the currency type
                BigDecimal totalReceived = transactionRepository.calculateTotalReceived(wallet.getId(),
                        TransactionType.CREDITED, currencyType, startDate, endDate);

                // Ensure totalReceived is not null
                if (totalReceived == null) {
                    totalReceived = BigDecimal.ZERO;
                }

                // Get maintenance fee percentage
                TransactionFee feeConfig = transactionFeeRepository.findFirstByOrderById();
                BigDecimal platformFeePercentage = feeConfig != null ? feeConfig.getMakerFeePercentage()
                        : BigDecimal.valueOf(0.0055);

                // Calculate maintenance fee
                BigDecimal maintenanceFee = totalReceived.multiply(platformFeePercentage);

                // Find existing debt for the user and currency type
                Optional<DebtCollector> existingDebt = debtCollectorRepository.findByUserIdAndCurrencyTypeAndDebtStatus(
                        wallet.getUserId(), currencyType, DebtStatus.PENDING);

                if (existingDebt.isPresent()) {
                    DebtCollector debt = existingDebt.get();
                    debt.setDueAmount(debt.getDueAmount().add(maintenanceFee));

                    // Update debt status based on wallet balance
                    if (walletBalance.compareTo(debt.getDueAmount()) < 0) {
                        debt.setDebtStatus(DebtStatus.OVERDUE);
                    } else {
                        debt.setDebtStatus(DebtStatus.PENDING);
                    }

                    debtCollectorRepository.save(debt);
                } else {
                       
                    // Create new debt if none exists
                    DebtCollector debt = new DebtCollector();
                    debt.setUserId(wallet.getUserId());
                    debt.setCurrencyType(enumCurrencyType);
                    debt.setAmount(maintenanceFee);
                    debt.setDueAmount(BigDecimal.ZERO);
                    debt.setDebtStatus(DebtStatus.PENDING);
                    debt.setDescription("Monthly maintenance fee");
                    debtCollectorRepository.save(debt);
                }
            }
        }
    }

}
