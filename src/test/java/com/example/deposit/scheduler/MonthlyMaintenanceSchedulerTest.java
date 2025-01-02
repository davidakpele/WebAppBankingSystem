package com.example.deposit.scheduler;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.example.deposit.enums.CurrencyType;
import com.example.deposit.enums.DebtStatus;
import com.example.deposit.enums.TransactionType;
import com.example.deposit.models.DebtCollector;
import com.example.deposit.models.Wallet;
import com.example.deposit.properties.MonthlyMaintenanceScheduler;
import com.example.deposit.repository.DebtCollectorRepository;
import com.example.deposit.repository.WalletRepository;
import com.example.deposit.repository.WalletTransanctionHistoryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MonthlyMaintenanceSchedulerTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransanctionHistoryRepository transactionRepository;

    @Mock
    private DebtCollectorRepository debtCollectorRepository;

    @InjectMocks
    private MonthlyMaintenanceScheduler scheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testNoExistingDebtAndSufficientBalance() {
        // Arrange
        Wallet userWallet = new Wallet();
        for (CurrencyType currencyType : CurrencyType.values()) {
            userWallet.getBalance().putIfAbsent(currencyType.name(), BigDecimal.ZERO);
        }
        CurrencyType request = CurrencyType.NGN;
        // Retrieve the current balance for the specific currency (default to 0 if not found)
        BigDecimal currentBalance = userWallet.getBalance().getOrDefault(request.name(), BigDecimal.ZERO);
       
        userWallet.setId(1L); // Ensure the ID is set
        
        BigDecimal newBalance = currentBalance.add(new BigDecimal("100.00"));
        // Update the balance in the wallet's balances map for the specified currency
        userWallet.getBalance().put(request.name(), newBalance);    
        when(walletRepository.findAll()).thenReturn(List.of(userWallet));
        when(transactionRepository.calculateTotalReceived(userWallet.getId(),
                TransactionType.DEPOSIT, request.toString(), LocalDate.now().minusDays(28), LocalDate.now()))
                .thenReturn(new BigDecimal("1000.00")); // Mocking to return a valid BigDecimal
        when(transactionRepository.calculateTotalSpent(userWallet.getId(), TransactionType.TRANSFER,
                LocalDate.now().minusDays(28), LocalDate.now()))
                .thenReturn(new BigDecimal("500.00"));
        when(debtCollectorRepository.findByUserIdAndDebtStatus(userWallet.getUserId(),
                DebtStatus.PENDING)).thenReturn(Optional.empty());

        // Act
        scheduler.calculateAndCreateDebts();

        // Assert
        verify(debtCollectorRepository, times(1)).save(any(DebtCollector.class));
    }

    @Test
    void testExistingPendingDebtAndSufficientBalance() {
        // Arrange
        Wallet userWallet = new Wallet();
        userWallet.setUserId(2L);
        for (CurrencyType currencyType : CurrencyType.values()) {
            userWallet.getBalance().putIfAbsent(currencyType.name(), BigDecimal.ZERO);
        }
        CurrencyType request = CurrencyType.NGN;
        // Retrieve the current balance for the specific currency (default to 0 if not
        // found)
        BigDecimal currentBalance = userWallet.getBalance().getOrDefault(request.name(), BigDecimal.ZERO);
        BigDecimal newBalance = currentBalance.add(new BigDecimal("200.00"));
        // Update the balance in the wallet's balances map for the specified currency
        userWallet.getBalance().put(request.name(), newBalance);
        DebtCollector existingDebt = new DebtCollector();
        existingDebt.setUserId(userWallet.getUserId());
        existingDebt.setDebtStatus(DebtStatus.PENDING);
        existingDebt.setDueAmount(new BigDecimal("20.00"));

        when(walletRepository.findAll()).thenReturn(List.of(userWallet));
        when(transactionRepository.calculateTotalReceived(userWallet.getUserId(), TransactionType.DEPOSIT, 
                request.toString(), LocalDate.now().minusDays(28), LocalDate.now()))
                .thenReturn(new BigDecimal("1000.00"));
        when(transactionRepository.calculateTotalSpent(userWallet.getUserId(), TransactionType.TRANSFER,
                LocalDate.now().minusDays(28), LocalDate.now()))
                .thenReturn(new BigDecimal("500.00"));
        when(debtCollectorRepository.findByUserIdAndDebtStatus(userWallet.getUserId(), DebtStatus.PENDING))
                .thenReturn(Optional.of(existingDebt));

        // Act
        scheduler.calculateAndCreateDebts();

        // Assert
        verify(debtCollectorRepository, times(1)).save(existingDebt);
        assertEquals(DebtStatus.PENDING, existingDebt.getDebtStatus());
        assertEquals(new BigDecimal("20.0000"), existingDebt.getDueAmount()); // 20 + 5.5
    }

    @Test
    void testExistingPendingDebtAndInsufficientBalance() {
        // Arrange
        Wallet userWallet = new Wallet();
        userWallet.setUserId(3L);
        for (CurrencyType currencyType : CurrencyType.values()) {
            userWallet.getBalance().putIfAbsent(currencyType.name(), BigDecimal.ZERO);
        }
        CurrencyType request = CurrencyType.NGN;
        // Retrieve the current balance for the specific currency (default to 0 if not
        // found)
        BigDecimal currentBalance = userWallet.getBalance().getOrDefault(request.name(), BigDecimal.ZERO);
        BigDecimal newBalance = currentBalance.add(new BigDecimal("200.00"));
        // Update the balance in the wallet's balances map for the specified currency
        userWallet.getBalance().put(request.name(), newBalance);
        DebtCollector existingDebt = new DebtCollector();
        existingDebt.setUserId(userWallet.getUserId());
        existingDebt.setDebtStatus(DebtStatus.PENDING);
        existingDebt.setDueAmount(new BigDecimal("20.00"));

        when(walletRepository.findAll()).thenReturn(List.of(userWallet));
        when(transactionRepository.calculateTotalReceived(userWallet.getUserId(), TransactionType.DEPOSIT, request.toString(),
                LocalDate.now().minusDays(28), LocalDate.now()))
                .thenReturn(new BigDecimal("1000.00"));
        when(transactionRepository.calculateTotalSpent(userWallet.getUserId(), TransactionType.TRANSFER,
                LocalDate.now().minusDays(28), LocalDate.now()))
                .thenReturn(new BigDecimal("500.00"));
        when(debtCollectorRepository.findByUserIdAndDebtStatus(userWallet.getUserId(), DebtStatus.PENDING))
                .thenReturn(Optional.of(existingDebt));

        // Act
        scheduler.calculateAndCreateDebts();

        // Assert
        verify(debtCollectorRepository, times(1)).save(existingDebt);
        assertEquals(DebtStatus.OVERDUE, existingDebt.getDebtStatus());
    }

    @Test
    void testNoUsersFound() {
        // Arrange
        when(walletRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        scheduler.calculateAndCreateDebts();

        // Assert
        verify(transactionRepository, never()).calculateTotalReceived(anyLong(), any(), any(), any(), any());
        verify(transactionRepository, never()).calculateTotalSpent(anyLong(), any(), any(), any());
        verify(debtCollectorRepository, never()).save(any());
    }
}
