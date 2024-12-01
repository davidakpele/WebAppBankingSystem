package com.example.deposit.properties;

import java.math.BigDecimal;
import java.util.List;
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
            Wallet wallet = walletRepository.findById(debt.getUserId()).orElse(null);
            if (wallet == null)
                continue;

            BigDecimal userBalance = wallet.getBalance();
            if (userBalance.compareTo(debt.getAmount()) >= 0) {
                // Deduct from user wallet and credit platform wallet
                wallet.setBalance(userBalance.subtract(debt.getAmount()));

                Revenue platformWallet = revenueRepository.findFirstByOrderById();
                platformWallet.setBalance(platformWallet.getBalance().add(debt.getAmount()));

                debt.setDebtStatus(DebtStatus.PAID);
                debtCollectorRepository.save(debt);
                walletRepository.save(wallet);
                revenueRepository.save(platformWallet);

                // Inform Users why there was a deduction in their wallet, inform them that it for service maintenance for the month.
                Long usersId = wallet.getUserId();
                // fetch users email from userServiceClient and send them notifications about the deduction that just happen .
                UserDTO user = userServiceClient.fetchPublicUserById(usersId);
                if (user != null) {
                    String content = "Dear " + user.getRecords().get(0).getFirstName() + ",\n\n"
                    +"We hope this message finds you well. This is to inform you that a deduction of " +
                    Refactory.formatBigDecimal(debt.getAmount()) + 
                    " has been made from your wallet balance as part of our monthly service maintenance fee.\n\n" +
                    "Your current wallet balance is " + Refactory.formatBigDecimal(wallet.getBalance()) + ".\n\n" +
                    "This maintenance fee helps us ensure the seamless operation of our platform and the continuous improvement of our services.\n\n" +
                    "Thank you for your understanding and continued trust in our services. If you have any questions, feel free to reach out to our support team.\n\n";
                    walletNotificationProducer.sendMaintenanceNotification(
                        user.getEmail(), 
                        user.getRecords().get(0).getFirstName(), 
                        debt.getAmount(),
                        wallet.getBalance(),
                        content
                    );
                }
            } else {
                // User does not have enough balance, mark as OVERDUE
                debt.setDebtStatus(DebtStatus.OVERDUE);
                debtCollectorRepository.save(debt);
            }
        }
    }
}
