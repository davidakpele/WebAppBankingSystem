package com.example.deposit.serviceImplementations;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.deposit.dto.CoinWalletDTO;
import com.example.deposit.dto.UserDTO;
import com.example.deposit.enums.BannedReasons;
import com.example.deposit.enums.CurrencyType;
import com.example.deposit.enums.TransactionStatus;
import com.example.deposit.enums.TransactionType;
import com.example.deposit.exceptions.UserNotFoundException;
import com.example.deposit.messageProducer.WalletNotificationProducer;
import com.example.deposit.middleware.TransactionMonitor;
import com.example.deposit.models.Revenue;
import com.example.deposit.models.Wallet;
import com.example.deposit.models.WalletTransactionHistory;
import com.example.deposit.payloads.ExternalTransactionRequest;
import com.example.deposit.payloads.TransactionRequest;
import com.example.deposit.platformService.FeeCalculatorService;
import com.example.deposit.repository.RevenueRepository;
import com.example.deposit.repository.WalletRepository;
import com.example.deposit.repository.WalletTransanctionHistoryRepository;
import com.example.deposit.services.WalletService;
import com.example.deposit.utils.KeyCollections;
import com.example.deposit.utils.Refactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import com.example.deposit.responses.Error;
import java.sql.Timestamp;

@Service
@RequiredArgsConstructor
public class WalletServiceImplementations implements WalletService{

    private final WalletRepository walletRepository;
    private final UserServiceClient userServiceClient;
    private final WalletTransanctionHistoryRepository walletTransanctionHistoryRepository;
    private final CoinWalletServiceClient coinWalletServiceClient;
    private final PasswordEncoder passwordEncoder;
    private final TransactionMonitor transactionMonitor;
    private final KeyCollections keyCollections;
    private final WalletNotificationProducer walletNotificationProducer;
    private final RevenueRepository revenueRepository;
    private final FeeCalculatorService feeCalculatorService;

    @Override
    public Wallet getWalletByUser(UserDTO user, String token) {
        UserDTO userDTO = userServiceClient.getUserById(user.getId(), token);
        return walletRepository.findByUserId(userDTO.getId())
                .orElseThrow(() -> new RuntimeException("Wallet not found for user ID: " + userDTO.getId()));
    }

    @Override
    public Optional<Wallet> getWalletByUserId(Long userId, String token) {
        // Fetch UserDTO if needed
        UserDTO user = userServiceClient.getUserById(userId, token);
        return walletRepository.findByUserId(user.getId());
    }

    @SuppressWarnings("deprecation")
    @Override
    public ResponseEntity<?> createTransferPin(TransactionRequest request, String token,
            Authentication authentication) {
        String requestUsername = authentication.getName();
        UserDTO user = userServiceClient.getUserByUsername(requestUsername, token);

        Optional<Wallet> wallet = walletRepository.findByUserId(user.getId());

        if (!user.getUsername().equals(requestUsername)) {
            return Error.createResponse(
                    "Fraudulent action is taken here, You are not the authorized user to operate this wallet.",
                    HttpStatus.FORBIDDEN,
                    "One more attempt from you again, you will be reported to the Economic and Financial Crimes Commission (EFCC).");
        }
        if (request.getTransferpin().isEmpty()) {
            return Error.createResponse("Your withdrawal/transfer pin is required*", HttpStatus.NOT_FOUND,
                    "Please provide your withdrawal/transfer pin and please don't share it with anyone for security reasons.");
        }
        String providedPin = request.getTransferpin();

        // Check if the pin is not exactly 4 digits or contains non-digit characters
        if (providedPin.length() != 4 || !providedPin.matches("\\d{4}")) {
            return Error.createResponse(
                    "Invalid input. Please provide exactly 4 digits.",
                    HttpStatus.BAD_REQUEST,
                    "Your pin must be exactly 4 numeric digits.");
        }

        if (wallet.isPresent()) {
            Wallet eWallet = wallet.get();

            // Set all balances to 0.00
            eWallet.getBalance().replaceAll((key, value) -> BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP));

            // Set userId and encoded transfer pin
            eWallet.setUserId(user.getId());
            eWallet.setPassword(passwordEncoder.encode(providedPin));

            // Save updated wallet
            walletRepository.save(eWallet);
        }

        // Update the user's transfer pin in their user record
        if (userServiceClient.UpdateUserTranferPinInUserRecord(token, user.getId(), true)) {
            return Error.createResponse(
                    "Withdrawal/transfer password successfully set, you can now make withdrawals or transfers.",
                    HttpStatus.OK, "Success");
        }

        return Error.createResponse("Sorry, this user does not exist in our system.", HttpStatus.BAD_REQUEST,
                "User does not exist, please provide a valid username.");
    }

    @SuppressWarnings("deprecation")
    @Override
    public ResponseEntity<?> getBalance(String username, String token) {
        // Fetch user details
        UserDTO user = userServiceClient.getUserByUsername(username, token);

        // Fetch main wallet balance
        Wallet wallet = walletRepository.findWalletByUserId(user.getId());

        // Default balance to zero if wallet is null
        Map<String, BigDecimal> walletBalances = wallet == null ? new HashMap<>() : wallet.getBalance();

        // Prepare list of wallet balances with currency code as key
        List<Map<String, Object>> balanceDetails = walletBalances.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> balanceInfo = new HashMap<>();
                    balanceInfo.put("currency_code", entry.getKey());
                    balanceInfo.put("balance", entry.getValue().setScale(2, BigDecimal.ROUND_HALF_UP));
                    return balanceInfo;
                })
                .collect(Collectors.toList());

        // Format central balance
        BigDecimal centralBalance = walletBalances.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, BigDecimal.ROUND_HALF_UP);

        String formattedBalance = Refactory.formatBigDecimal(centralBalance);

        // Fetch transaction history count
        Long transactionCount = walletTransanctionHistoryRepository.countByUserId(user.getId());
        String transactionHistoryLabel = transactionCount > 1 ? transactionCount + " times" : "once";

        // Fetch coin wallets using CoinWalletService
        List<CoinWalletDTO> coinWallets = coinWalletServiceClient.getCoinWalletsByUserId(user.getId(), token);

        List<Map<String, Object>> coinWalletDetails = coinWallets.stream()
                .map(coinWallet -> {
                    Map<String, Object> coinDetails = new HashMap<>();
                    coinDetails.put("cryptoId", coinWallet.getCryptoId());
                    coinDetails.put("balance", coinWallet.getBalance().setScale(2, BigDecimal.ROUND_HALF_UP));
                    coinDetails.put("walletAddress", coinWallet.getWalletAddress());
                    return coinDetails;
                })
                .collect(Collectors.toList());

        // Build the response
        Map<String, Object> response = new HashMap<>();
        response.put("central_balance", formattedBalance);
        response.put("transaction_history_count", transactionHistoryLabel);
        response.put("wallet_balances", balanceDetails); // Added wallet balances here
        response.put("coin_wallets", coinWalletDetails);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ResponseEntity<?> getBalanceByCurrencyType(Long userId, CurrencyType currency, String token) {
        try {
            // Fetch user details
            UserDTO user = userServiceClient.getUserById(userId, token);
            
            if (user == null) {
                throw new UserNotFoundException("User not found: No user data returned for ID " + userId, null);
            }

            // Fetch main wallet balance
            Wallet wallet = walletRepository.findWalletByUserIdAndCurrencyCode(user.getId(), currency.toString());
            BigDecimal balance = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);

            if (wallet != null) {
                BigDecimal currencyBalance = wallet.getBalance().get(currency.toString());
                if (currencyBalance != null) {
                    balance = currencyBalance.setScale(2, BigDecimal.ROUND_HALF_UP);
                }
            }
 
            // Format balance
            String formattedBalance = Refactory.formatBigDecimal(balance);

            // Build the response
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("currency", currency);
            response.put("formated_balance", formattedBalance); 
            response.put("balance", balance);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (UserNotFoundException ex) {
            // Handle user not found error
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "User not found");
            errorResponse.put("details", ex.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);

        } catch (RuntimeException ex) {
            // Handle general server errors
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Server error");
            errorResponse.put("details", ex.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Wallet getUserWalletByUserWalletId(Long userId) {
        return walletRepository.findByUserId(userId).orElse(null);
    }

    @Override
    public ResponseEntity<?> TransferWithUsernameToUserInSamePlatform(
            String username, TransactionRequest request,
            Authentication authentication, String token, HttpServletRequest httpRequest) {
        try {
            UserDTO fromUser = userServiceClient.getUserByUsername(username, token);
            UserDTO toUser = userServiceClient.findUserByUsernameInPublicRoute(request.getToUser());

            if (fromUser == null) {
                return Error.createResponse("User not found", HttpStatus.BAD_REQUEST,
                        "Sender user does not exist, please provide a valid username.");
            }

            if (fromUser != null && !fromUser.getUsername().equals(request.getFromUser())) {
                return Error.createResponse(
                        "Fraudulent action detected. You are not authorized to operate this wallet.",
                        HttpStatus.FORBIDDEN,
                        "One more attempt and you will be reported to the Economic and Financial Crimes Commission (EFCC).");
            }

            boolean isLockedAccount = fromUser.getRecords().get(0).isLocked();
            boolean isBlockedAccount = fromUser.getRecords().get(0).isBlocked();

            if (isLockedAccount) {
                return Error.createResponse(
                        "Your account has been temporarily locked. Please contact support.",
                        HttpStatus.FORBIDDEN, "Please contact support to unlock your account.");
            }

            if (isBlockedAccount) {
                return Error.createResponse(
                        "Your account has been temporarily banned. Please contact support.",
                        HttpStatus.FORBIDDEN, "Please contact support to verify your account.");
            }

            if (toUser == null) {
                return Error.createResponse("Receiver not found", HttpStatus.BAD_REQUEST,
                        "The recipient username does not exist in our system.");
            }

            Wallet fromKeyWalletAccount = getUserWalletByUserWalletIdAndCurrency(fromUser.getId(),
                    request.getCurrencyType());
            Wallet toKeyWalletAccount = getUserWalletByUserWalletIdAndCurrency(toUser.getId(),
                    request.getCurrencyType());

            // Check suspicious behavior
            if (transactionMonitor.isHighVolumeOrFrequentTransactions(fromUser.getId())) {
                return Error.createResponse("Account temporarily banned due to suspicious activity.",
                        HttpStatus.FORBIDDEN, "Please contact support.");
            }

            if (transactionMonitor.isHighRiskRegion(request)) {
                return Error.createResponse("Account temporarily banned due to high-risk transactions.",
                        HttpStatus.FORBIDDEN, "Please contact support.");
            }

            List<WalletTransactionHistory> activities =  walletTransanctionHistoryRepository.findByTimestampAfterAndWalletId(Instant.now().minus(1, ChronoUnit.MINUTES), fromKeyWalletAccount.getId());
            // detect suspicious patterns
            for (WalletTransactionHistory activity : activities) {
                if (activity.getType() == TransactionType.DEPOSIT
                        && activity.getTimestamp().after(Timestamp.from((Instant.now().minus(1, ChronoUnit.MINUTES))))) {
                    // update user account status
                        userServiceClient.updateUserAccountStatus(fromUser.getId(), BannedReasons.SUSPICIOUS_ACTIVITY, token);
                }
            }
            // Verify transfer pin
            String providedPin = request.getTransferpin().trim();
            if (providedPin == null || providedPin.isEmpty()) {
                return Error.createResponse("Transfer pin is required.", HttpStatus.BAD_REQUEST,
                        "Please provide your transfer pin.");
            }

            if (!passwordEncoder.matches(providedPin, fromKeyWalletAccount.getPassword())) {
                return Error.createResponse("Invalid transfer pin.", HttpStatus.UNAUTHORIZED,
                        "The provided transfer pin is incorrect.");
            }
 
            BigDecimal feePercentage = feeCalculatorService.calculateFee(request.getAmount());
            BigDecimal feeAmount = request.getAmount().multiply(feePercentage);
            BigDecimal finalDeduction = feeAmount.add(request.getAmount());

            Map<String, BigDecimal> fromBalance = fromKeyWalletAccount.getBalance();
            String currencyKey = request.getCurrencyType().toString().toUpperCase();
            BigDecimal senderBalance = fromBalance.getOrDefault(currencyKey, BigDecimal.ZERO);

            if (senderBalance.compareTo(finalDeduction) < 0) {
                return Error.createResponse("Insufficient balance", HttpStatus.BAD_REQUEST,
                        "Your account balance is too low.");
            }

            // Handle recipient wallet creation if it doesn't exist
            if (toKeyWalletAccount == null) {
                toKeyWalletAccount = new Wallet();
                toKeyWalletAccount.setUserId(toUser.getId());

                Map<String, BigDecimal> newBalances = new HashMap<>();
                for (CurrencyType currency : CurrencyType.values()) {
                    newBalances.put(currency.name(), BigDecimal.ZERO);
                }
                toKeyWalletAccount.setBalance(newBalances);
                walletRepository.save(toKeyWalletAccount);
            }

            // Update sender's balance
            fromBalance.put(currencyKey, senderBalance.subtract(finalDeduction));
            fromKeyWalletAccount.setBalance(fromBalance);

            // Update recipient's balance
            Map<String, BigDecimal> toBalance = toKeyWalletAccount.getBalance();
            BigDecimal recipientBalance = toBalance.getOrDefault(currencyKey, BigDecimal.ZERO);
            toBalance.put(currencyKey, recipientBalance.add(request.getAmount()));
            toKeyWalletAccount.setBalance(toBalance);

            // Save updated wallets
            walletRepository.save(fromKeyWalletAccount);
            walletRepository.save(toKeyWalletAccount);

            // Record transaction history
            recordTransactionHistory(fromKeyWalletAccount, toKeyWalletAccount, request, fromUser, toUser);

            // Process platform fee deductions
            addRevenue(feeAmount, request.getCurrencyType());

            // Send notifications
            sendTransactionNotifications(fromUser, toUser, request.getAmount(), feeAmount, request.getCurrencyType());

            return Error.createResponse("Transaction successful", HttpStatus.OK,
                    request.getAmount() + " has been successfully sent to " + request.getToUser() + ".");
        } catch (Exception ex) {
            return Error.createResponse("An unexpected error occurred.", HttpStatus.INTERNAL_SERVER_ERROR,
                    ex.getMessage());
        }
    }

    private Wallet getUserWalletByUserWalletIdAndCurrency(Long userId, CurrencyType currencyType) {
        return walletRepository.findWalletByUserIdAndCurrencyCode(userId, currencyType.toString());
    }

    @Override
    public ResponseEntity<?> transferToExternalUserOutSidePlatform(String username, ExternalTransactionRequest request,
            Authentication authentication, String token, HttpServletRequest httpReques) {
        UserDTO fromUser = userServiceClient.getUserByUsername(username, token);

        if (fromUser ==null) {
            return Error.createResponse("Sorry this user does not exist in our system.",
                    HttpStatus.BAD_REQUEST,
                    "Sender User does not exist, please provide valid username.");
        }

        if (fromUser != null && !fromUser.getUsername().equals(request.getFromUser())) {
            return Error.createResponse(
                    "Fraudulent action is taken here, You are not the authorized user to operate this wallet.",
                    HttpStatus.FORBIDDEN,
                    "One more attempt from you again, you will be reported to the Economic and Financial Crimes Commission (EFCC).");
        }
        
        // String requestUsername = authentication.getName();

        boolean isLockedAccount = fromUser.getRecords().get(0).isLocked();

        if (isLockedAccount) {
            return Error.createResponse(
                    "Your account has been temporarily locked. Please reach out to our support team to unlock your account.",
                    HttpStatus.FORBIDDEN,
                    "Please reach out to our support team to unlock your account.");
        }

        boolean isAccountBlocked = fromUser.getRecords().get(0).isBlocked();
        if (isAccountBlocked) {
            return Error.createResponse(
                    "Your account has been blocked due to security concerns. Contact our customer service for assistance with your blocked account.",
                    HttpStatus.FORBIDDEN,
                    "Contact our customer service for assistance with your blocked account.");
        }

        Wallet fromKeyWalletAccount = getUserWalletByUserWalletId(fromUser.getId());

        // Check for suspicious behaviors
        if (transactionMonitor.isHighVolumeOrFrequentTransactions(fromUser.getId())) {
            return Error.createResponse("Account temporarily banned due to high volume of transactions.",
                    HttpStatus.FORBIDDEN,
                    "Please contact support.");
        }

        if (transactionMonitor.isUnverifiedOrNewWallet(fromUser.getId(), token)) {
            return Error.createResponse("Account temporarily banned due to unverified or newly created wallet.",
                    HttpStatus.FORBIDDEN,
                    "Please contact support.");
        }

        if (transactionMonitor.isImmediateTransferAfterDeposit(fromUser.getId())) {
            return Error.createResponse("Account temporarily banned due to immediate transfers following deposits.",
                    HttpStatus.FORBIDDEN,
                    "Please contact support.");
        }

        if (transactionMonitor.isFromBlacklistedAddress(fromKeyWalletAccount.getId())) {
            return Error.createResponse("Transaction blocked due to blacklisted wallet address.", HttpStatus.FORBIDDEN,
                    "Please contact support.");
        }

        // Verify user transfer pin password
        String providedPin = request.getTransferpin().trim();

        if (providedPin == null || providedPin.isEmpty()) {
            return Error.createResponse("Transfer pin is required.", HttpStatus.BAD_REQUEST,
                    "Please provide your transfer pin.");
        }

        if (!passwordEncoder.matches(providedPin, fromKeyWalletAccount.getPassword())) {
            return Error.createResponse("Invalid transfer pin.", HttpStatus.UNAUTHORIZED,
                    "The provided transfer pin is incorrect.");
        }
        List<WalletTransactionHistory> activities = walletTransanctionHistoryRepository.findByTimestampAfterAndWalletId(
                Instant.now().minus(1, ChronoUnit.MINUTES), fromKeyWalletAccount.getId());
        // detect suspicious patterns
        for (WalletTransactionHistory activity : activities) {
            if (activity.getType() == TransactionType.DEPOSIT
                    && activity.getTimestamp().after(Timestamp.from((Instant.now().minus(1, ChronoUnit.MINUTES))))) {
                // update user account status
                userServiceClient.updateUserAccountStatus(fromUser.getId(), BannedReasons.SUSPICIOUS_ACTIVITY, token);
            }
        }
        // get platform fee amount
        BigDecimal feePercentage = feeCalculatorService.calculateFee(request.getAmount());
        // Calculate the fee based on the transfer amount
        BigDecimal feeAmount = request.getAmount().multiply(feePercentage);
        // Calculate the total deduction (amount + fee)
        BigDecimal finalDeduction = feeAmount.add(request.getAmount());

        if (fromKeyWalletAccount != null) {
            if (((BigDecimal) fromKeyWalletAccount.getBalance()).compareTo(finalDeduction) < 0) {
                return Error.createResponse("Insufficient balance", HttpStatus.BAD_REQUEST,
                        "Your account balance is low.");
            } else {

                // substract the amount from sender and set into recipient bank provided by the
                // sender
                // fromKeyWalletAccount.setBalance(fromKeyWalletAccount.getBalance().subtract(request.getAmount()));

                // walletRepository.save(fromKeyWalletAccount);

                // now initiate payment transfer with any gateway service, flutter, or paystack
                // to transfer the money to user bank.

            }

            // Long fromTransactionUniqueId = uniqueKeyGenerator.createSnowflakeUniqueId();
            // eNairaTransaction fromTransaction = new eNairaTransaction();
            // fromTransaction.setId(fromTransactionUniqueId);
            // fromTransaction.setWallet(fromKeyWalletAccount);
            // fromTransaction.setAmount(request.getAmount().negate());
            // fromTransaction.setType(TransactionType.TRANSFER);
            // fromTransaction.setMessage(TransactionMessage.Successful.toString());
            // fromTransaction.setStatus(TransactionStatus.Success.toString());
            // fromTransaction.setDescription(TransactionType.DEBITED.toString());
            // transactionRepository.save(fromTransaction);

            // process platform fees deductions
            // addRevenue(feeAmount);
            // Wallet SenderTotalBalance =
            // getUserWalletByUserWalletId(fromUser.get().getId());

            // String senderEmail = fromUser.get().getEmail();

            // BigDecimal transferAmount = request.getAmount();

            // Optional<UserRecord> senderName =
            // userRecordImplementation.getUserNames(fromUser.get().getId());

            // notification
            // String senderFullName = senderName.get().getFirstName() + " " +
            // senderName.get().getLastName();

            // CompletableFuture
            // .runAsync(() ->
            // emailServiceImplementation.sendDebitWalletNotificationToRecipient(senderEmail,
            // feeAmount, transferAmount,
            // senderFullName, null, SenderTotalBalance.getBalance()))
            // .exceptionally(
            // ex -> handleAsyncError("Failed to send transaction notification to recipient
            // email.", ex));

        }
        return Error.createResponse("Transaction successful", HttpStatus.OK, null);
    }

    public void addRevenue(BigDecimal feeAmount, CurrencyType currencyType) {
        Revenue existingRevenue = revenueRepository.findFirstByOrderById();

        if (existingRevenue != null) {
            // Get the current balance for the desired currency (e.g., "USD")
            Map<String, BigDecimal> currentBalance = existingRevenue.getBalance();

            // If the balance map already contains the currency, add the fee amount to it
            String currencyCode = currencyType.toString(); 
            BigDecimal currentCurrencyBalance = currentBalance.getOrDefault(currencyCode, BigDecimal.ZERO);
            currentBalance.put(currencyCode, currentCurrencyBalance.add(feeAmount));

            existingRevenue.setBalance(currentBalance);
            existingRevenue.setUpdatedAt(LocalDateTime.now());
            revenueRepository.save(existingRevenue);
        } else {
            // If no revenue record exists, create a new one
            Revenue newRevenue = new Revenue();
            newRevenue.setId(keyCollections.createSnowflakeUniqueId());

            // Create a new balance map and add the initial balance for the currency
            Map<String, BigDecimal> initialBalance = new HashMap<>();
            initialBalance.put(currencyType.toString(), feeAmount);

            newRevenue.setBalance(initialBalance);
            newRevenue.setUpdatedAt(LocalDateTime.now());
            revenueRepository.save(newRevenue);
        }
    }

    private Void handleAsyncError(String errorMessage, Throwable ex) {
        System.out.println(errorMessage + ": " + ex.getMessage());
        return null;
    }

    private void recordTransactionHistory(Wallet fromKeyWalletAccount, Wallet toKeyWalletAccount,
            TransactionRequest request, UserDTO fromUser, UserDTO toUser) {
        String formattedTransactionType = Refactory.formatEnumValue(TransactionType.WALLET_TO_WALLET_TRANSFER);
        Long fromTransactionUniqueId = keyCollections.createSnowflakeUniqueId();
        Long toTransactionUniqueId = keyCollections.createSnowflakeUniqueId();

        String FromsessionId = keyCollections.generateSessionId();

        WalletTransactionHistory fromTransaction = new WalletTransactionHistory();
        fromTransaction.setId(fromTransactionUniqueId);
        fromTransaction.setWallet(fromKeyWalletAccount);
        fromTransaction.setSessionId(FromsessionId);
        fromTransaction.setAmount(request.getAmount().negate());
        fromTransaction.setType(TransactionType.DEBITED);
        fromTransaction.setCurrencyType(request.getCurrencyType());
        fromTransaction.setMessage("TRANSFERED " +request.getCurrencyType() +request.getAmount()+ " TO " + toUser.getUsername());
        fromTransaction.setStatus(TransactionStatus.Success.toString());
        fromTransaction.setDescription(formattedTransactionType);
        walletTransanctionHistoryRepository.save(fromTransaction);

        String TosessionId = keyCollections.generateSessionId();

        WalletTransactionHistory toTransaction = new WalletTransactionHistory();
        toTransaction.setId(toTransactionUniqueId);
        toTransaction.setWallet(toKeyWalletAccount);
        toTransaction.setSessionId(TosessionId);
        toTransaction.setAmount(request.getAmount());
        toTransaction.setMessage("TRANSFER "  +request.getCurrencyType() +request.getAmount()+ " FROM " + fromUser.getUsername());
        toTransaction.setStatus(TransactionStatus.Success.toString());
        toTransaction.setCurrencyType(request.getCurrencyType());
        toTransaction.setType(TransactionType.CREDITED);
        toTransaction.setDescription(formattedTransactionType);
        walletTransanctionHistoryRepository.save(toTransaction);
    }

    private void sendTransactionNotifications(UserDTO fromUser, UserDTO toUser, BigDecimal amount,
            BigDecimal feeAmount, CurrencyType currencyType) {
        Wallet SenderTotalBalance = getUserWalletByUserWalletId(fromUser.getId());
        Wallet RecipientTotalBalance = getUserWalletByUserWalletId(toUser.getId());

        String senderEmail = fromUser.getEmail();
        String recipientEmail = toUser.getEmail();

        // notification
        String senderFullName = fromUser.getRecords().get(0).getFirstName() + " "
                + fromUser.getRecords().get(0).getLastName();

        String receiverFullName = toUser.getRecords().get(0).getFirstName() + " "+ toUser.getRecords().get(0).getLastName();
        
        BigDecimal recipientTotalBalance = RecipientTotalBalance.getBalance().get(currencyType.toString());

        BigDecimal sendTotalBalance = SenderTotalBalance.getBalance().get(currencyType.toString());
        // The Recipient receive credit alert notification message
        CompletableFuture
                .runAsync(() -> walletNotificationProducer.sendCreditWalletNotification(recipientEmail, 
                        amount,
                        senderFullName, receiverFullName, recipientTotalBalance))
                .exceptionally(ex -> handleAsyncError("Failed to send transaction notification to sender email.", ex));
        // The sender receive debit alert notification message
        CompletableFuture
                .runAsync(() -> walletNotificationProducer.sendDebitWalletNotification(senderEmail, feeAmount,
                        amount, senderFullName, receiverFullName, sendTotalBalance))
                .exceptionally(ex -> handleAsyncError("Failed to send transaction notification to sender email.", ex));
    }

    @Override
    public ResponseEntity<?> creditWalletFromCryptoTrade(TransactionRequest request, String username, String token) {
        try {
            Wallet buyerWallet = walletRepository.findWalletByUserId(request.getCreditorUserId());
            if (buyerWallet !=null) {
                Wallet recipientWallet = walletRepository.findWalletByUserId(request.getRecipientUserId());
                if (recipientWallet ==null) {
                    return Error.createResponse("Sorry this seller wallet does not exist in our system.",
                            HttpStatus.BAD_REQUEST,
                            "Seller User does not exist.");
                }

                Map<String, BigDecimal> fromBalance = buyerWallet.getBalance();
                String currencyKey = request.getCurrencyType().toString().toUpperCase();
                BigDecimal transferBalance = fromBalance.getOrDefault(currencyKey, BigDecimal.ZERO);

                if (transferBalance.compareTo(request.getAmount()) < 0) {
                    return Error.createResponse("Insufficient balance", HttpStatus.BAD_REQUEST,
                            "Buyer account balance is too low.");
                }
                UserDTO toUser = userServiceClient.fetchPublicUserById(request.getRecipientUserId());
                
                UserDTO fromUser = userServiceClient.fetchPublicUserById(request.getCreditorUserId());

                // Update sender's balance
                fromBalance.put(currencyKey, transferBalance.subtract(request.getAmount()));
                buyerWallet.setBalance(fromBalance);

                // Update buyer balance
                Map<String, BigDecimal> toBalance = recipientWallet.getBalance();
                BigDecimal recipientBalance = toBalance.getOrDefault(currencyKey, BigDecimal.ZERO);
                toBalance.put(currencyKey, recipientBalance.add(request.getAmount()));
                recipientWallet.setBalance(toBalance);

                // Save updated wallets
                walletRepository.save(recipientWallet);
                walletRepository.save(buyerWallet);

                // Record transaction history
                recordTransactionHistory(buyerWallet, recipientWallet, request, fromUser, toUser);

                // Process platform fee deductions
                CurrencyType currencyType = CurrencyType.valueOf(request.getCurrencyType().toString().toUpperCase());
                addRevenue(request.getProfit(), currencyType);

                // Send notifications
                sendTransactionNotifications(fromUser, toUser, request.getAmount(), request.getProfit(), request.getCurrencyType());

                return Error.createResponse("Transaction successful", HttpStatus.OK,
                        request.getAmount() + " has been successfully sent to " + fromUser.getUsername() + ".");
            
                }
            
            return Error.createResponse("Sorry this buyer does not exist in our system.",
                    HttpStatus.BAD_REQUEST,
                    "Buyer User does not exist.");
        } catch (Exception ex) {
            return Error.createResponse("An unexpected error occurred.", HttpStatus.INTERNAL_SERVER_ERROR,
                    ex.getMessage());
        }
    }

} 