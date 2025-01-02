package com.example.deposit.properties;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.example.deposit.dto.UserDTO;
import com.example.deposit.enums.DebtStatus;
import com.example.deposit.messageProducer.WalletNotificationProducer;
import com.example.deposit.models.DebtCollector;
import com.example.deposit.models.Revenue;
import com.example.deposit.models.Wallet;
import com.example.deposit.repository.DebtCollectorRepository;
import com.example.deposit.repository.RevenueRepository;
import com.example.deposit.repository.WalletRepository;
import com.example.deposit.serviceImplementations.UserServiceClient;
import com.example.deposit.utils.Refactory;
import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class DebtDeductionScheduler {

    private final WalletRepository walletRepository;
    private final DebtCollectorRepository debtCollectorRepository;
    private final RevenueRepository revenueRepository;
    private final UserServiceClient userServiceClient;
    private final WalletNotificationProducer walletNotificationProducer;

    @Scheduled(fixedRate = 5000) // Every 5 seconds
    public void deductPendingDebts() {
        List<DebtCollector> debts = debtCollectorRepository.findByDebtStatus(DebtStatus.PENDING);

        for (DebtCollector debt : debts) {
            // Fetch the wallet by userId
            Wallet wallet = walletRepository.findByUserId(debt.getUserId()).orElse(null);
            if (wallet == null)
                continue;

            // Access the wallet's balance for the specified currency
            Map<String, BigDecimal> balances = wallet.getBalance();
            String currencyCode = debt.getCurrencyType().name(); // Get currency as a string (e.g., "USD")

            BigDecimal currentBalance = balances.getOrDefault(currencyCode, BigDecimal.ZERO);
            if (currentBalance.compareTo(debt.getAmount()) >= 0) {
                // Deduct from the specific currency balance
                balances.put(currencyCode, currentBalance.subtract(debt.getAmount()));

                // Update platform wallet revenue for the same currency
                Revenue platformWallet = revenueRepository.findFirstByOrderById();
                Map<String, BigDecimal> platformBalances = platformWallet.getBalance();
                BigDecimal platformCurrentBalance = platformBalances.getOrDefault(currencyCode, BigDecimal.ZERO);
                platformBalances.put(currencyCode, platformCurrentBalance.add(debt.getAmount()));

                // Update debt status and save changes
                debt.setDebtStatus(DebtStatus.PAID);
                debtCollectorRepository.save(debt);
                walletRepository.save(wallet);
                revenueRepository.save(platformWallet);

                // Send notification to the user
                Long userId = wallet.getUserId();
                UserDTO user = userServiceClient.fetchPublicUserById(userId);
                if (user != null) {
                    String content = "Dear " + user.getRecords().get(0).getFirstName() + ",\n\n"
                            + "We hope this message finds you well. This is to inform you that a deduction of "
                            + Refactory.formatBigDecimal(debt.getAmount()) + " " + currencyCode
                            + " has been made from your wallet balance as part of our monthly service maintenance fee.\n\n"
                            + "Your current wallet balance for " + currencyCode + " is "
                            + Refactory.formatBigDecimal(balances.get(currencyCode)) + ".\n\n"
                            + "This maintenance fee helps us ensure the seamless operation of our platform and the continuous improvement of our services.\n\n"
                            + "Thank you for your understanding and continued trust in our services. If you have any questions, feel free to reach out to our support team.\n\n";
                    walletNotificationProducer.sendMaintenanceNotification(
                            user.getEmail(),
                            user.getRecords().get(0).getFirstName(),
                            debt.getAmount(),
                            balances.get(currencyCode),
                            content);
                }
            } else {
                // User does not have enough balance, mark debt as OVERDUE
                debt.setDebtStatus(DebtStatus.OVERDUE);
                debtCollectorRepository.save(debt);
            }
        }
    }

}
