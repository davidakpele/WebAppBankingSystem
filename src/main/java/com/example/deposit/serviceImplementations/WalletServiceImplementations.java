package com.example.deposit.serviceImplementations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.deposit.dto.CoinWalletDTO;
import com.example.deposit.dto.UserDTO;
import com.example.deposit.enums.TransactionStatus;
import com.example.deposit.enums.TransactionType;
import com.example.deposit.exceptions.UserNotFoundException;
import com.example.deposit.messageProducer.WalletNotificationProducer;
import com.example.deposit.middleware.TransactionMonitor;
import com.example.deposit.models.Revenue;
import com.example.deposit.models.TransactionFee;
import com.example.deposit.models.Wallet;
import com.example.deposit.models.WalletTransactionHistory;
import com.example.deposit.payloads.ExternalTransactionRequest;
import com.example.deposit.payloads.TransactionRequest;
import com.example.deposit.repository.RevenueRepository;
import com.example.deposit.repository.TransactionFeeRepository;
import com.example.deposit.repository.WalletRepository;
import com.example.deposit.repository.WalletTransanctionHistoryRepository;
import com.example.deposit.services.WalletService;
import com.example.deposit.utils.KeyCollections;
import com.example.deposit.utils.Refactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import com.example.deposit.responses.Error;

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
    private final TransactionFeeRepository transactionFeeRepository;
    
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

    @Override
    public ResponseEntity<?> createTransferPin(TransactionRequest request, String token, Authentication authentication) {
        String requestUsername = authentication.getName();
        UserDTO user = userServiceClient.getUserByUsername(requestUsername, token);
        
        Optional<Wallet> wallet =walletRepository.findByUserId(user.getId());

        if (!user.getUsername().equals(requestUsername)) {
            return Error.createResponse(
                    "Fraudulent action is taken here, You are not the authorized user to operate this wallet.",
                    HttpStatus.FORBIDDEN,
                    "One more attempt from you again, you will be reported to the Economic and Financial Crimes Commission (EFCC).");
        }
        if (request.getTransferpin().isEmpty()) {
            return Error.createResponse("Your withdrawal/transfer pin is require*", HttpStatus.NOT_FOUND,
                    "Please provide your withdrawal/transafer pin and please don't share it with anyone for security reasons.");
        }
        String providedPin = request.getTransferpin();

        // Check if the pin is not exactly 4 digits or contains non-digit characters
        if (providedPin.length() != 4 || !providedPin.matches("\\d{4}")) {
            return Error.createResponse(
                    "Invalid input. Please provide exactly 4 digits.",
                    HttpStatus.BAD_REQUEST,
                    "Your pin must be exactly 4 numeric digits.");
        }
        if (wallet != null) {
            Wallet eWallet = wallet.get();
            eWallet.setUserId(user.getId());
            eWallet.setBalance(BigDecimal.ZERO);
            eWallet.setPassword(passwordEncoder.encode(providedPin));
            walletRepository.save(eWallet);
        }

        if (userServiceClient.UpdateUserTranferPinInUserRecord(token, user.getId(), true)) {
            return Error.createResponse(
                    "Withdrawal /transfer password successful set, you can now make withraws or transfer.",
                    HttpStatus.OK, "Success");
        }
        
        return Error.createResponse("Sorry this user does not exist in our system.", HttpStatus.BAD_REQUEST,
                "User does not exist, please provide valid username.");
    }

    @SuppressWarnings("deprecation")
    @Override
    public ResponseEntity<?> getBalance(String username, String token) {
        // Fetch user details
        UserDTO user = userServiceClient.getUserByUsername(username, token);

        // Fetch main wallet balance
        Wallet wallet = walletRepository.findWalletByUserId(user.getId());
        BigDecimal balance = wallet == null
                ? BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP)
                : wallet.getBalance().setScale(2, BigDecimal.ROUND_HALF_UP);
        System.out.println(balance);
        // Format balance and transaction count
        String formattedBalance = Refactory.formatBigDecimal(balance);
        
        Long transactionCount = walletTransanctionHistoryRepository.countByUserId(user.getId());
        String transactionHistoryLabel;
        if (transactionCount > 1) {
            transactionHistoryLabel = transactionCount + " times";
        } else {
            transactionHistoryLabel = "once";
        }
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
                .toList();

        // Build the response
        Map<String, Object> response = new HashMap<>();
        response.put("central_balance", formattedBalance);
        response.put("transaction_history_count", transactionHistoryLabel);
        response.put("coin_wallets", coinWalletDetails);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public Wallet getUserWalletByUserWalletId(Long userId) {
        return walletRepository.findByUserId(userId).orElse(null);
    }

    @Override
    public ResponseEntity<?> TransferWithUsernameToUserInSamePlatform(String username, TransactionRequest request, Authentication authentication, String token, HttpServletRequest httpRequest) {
        try {
            UserDTO fromUser = userServiceClient.getUserByUsername(username, token);
            UserDTO toUser = userServiceClient.findUserByUsernameInPublicRoute(request.getToUser());
            
            if (fromUser == null) {
                return Error.createResponse(
                        "User not found",
                        HttpStatus.BAD_REQUEST,
                        "Sender user does not exist, please provide a valid username.");
            }

            if (fromUser != null && !fromUser.getUsername().equals(request.getFromUser())) {
                return Error.createResponse(
                        "Fraudulent action is taken here, You are not the authorized user to operate this wallet.",
                        HttpStatus.FORBIDDEN,
                        "One more attempt from you again, you will be reported to the Economic and Financial Crimes Commission (EFCC).");
            }

            boolean isLockedAccount = fromUser.getRecords().get(0).isLocked();
            boolean isBlockedAccount = fromUser.getRecords().get(0).isBlocked();

            if (isLockedAccount) {
                return Error.createResponse(
                        "Your account has been temporarily locked. Please reach out to our support team to unlock your account.",
                        HttpStatus.FORBIDDEN,
                        "Please reach out to our support team to unlock your account.");
            }

            if (isBlockedAccount) {
                return Error.createResponse(
                        "Your account has been temporarily banned which prevent you for making am any transactions at the moment. Please reach out to our support team to unlock your account.",
                        HttpStatus.FORBIDDEN,
                        "Please reach out to our support team to verify your account.");
            }

            if (toUser == null) {
                return Error.createResponse("Sorry " + request.getToUser() + " user does not exist in our system.",
                        HttpStatus.BAD_REQUEST,
                        "Receiver User does not exist, please provide valid username.");
            }

            Wallet fromKeyWalletAccount = getUserWalletByUserWalletId(fromUser.getId());
            Wallet toKeyWalletAccount = getUserWalletByUserWalletId(toUser.getId());

            // Check for suspicious behaviors
            if (transactionMonitor.isHighVolumeOrFrequentTransactions(fromUser.getId())) {
                return Error.createResponse("Account temporarily banned due to high volume of transactions.",
                        HttpStatus.FORBIDDEN,
                        "Please contact support.");
            }

            if (transactionMonitor.isHighRiskRegion(request)) {
                return Error.createResponse("Account temporarily banned due to transactions from high-risk regions.",
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

            // Verify user transfer pin password
            String providedPin = request.getTransferpin().trim();

            if (providedPin == null || providedPin.isEmpty()) {
                return Error.createResponse("Transfer pin is required.", HttpStatus.BAD_REQUEST,
                        "Please provide your transfer pin.");
            }
            // if user tries to move money from wallet to same wallet.
            if (fromUser.getUsername().equals(request.getToUser())) {
                return Error.createResponse(
                        "Sorry, You request is not acceptable in our platform. You are making a requet to send "
                                + request.getAmount() + " to you own very wallet that belongs to your.",
                        HttpStatus.BAD_REQUEST,
                        "You can send money from you to wallet to your wallet.");
            }

            if (!passwordEncoder.matches(providedPin, fromKeyWalletAccount.getPassword())) {
                return Error.createResponse("Invalid transfer pin.", HttpStatus.UNAUTHORIZED,
                        "The provided transfer pin is incorrect.");
            }

            // get platform fee amount
            BigDecimal feePercentage = getFeePercentage();
            // Calculate the fee based on the transfer amount
            BigDecimal feeAmount = request.getAmount().multiply(feePercentage);
            // Calculate the total deduction (amount + fee)
            BigDecimal finalDeduction = feeAmount.add(request.getAmount());

            if (fromKeyWalletAccount == null || fromKeyWalletAccount.getBalance().compareTo(finalDeduction) < 0) {
                return Error.createResponse("Insufficient balance", HttpStatus.BAD_REQUEST,
                        "Your account balance is low.");
            }

            Wallet toWallet;

            if (toKeyWalletAccount != null) {
                toWallet = toKeyWalletAccount;
            } else {

                toWallet = new Wallet();
                toWallet.setUserId(toUser.getId());
                toWallet.setBalance(BigDecimal.ZERO);
            }

            Wallet fromWallet = fromKeyWalletAccount;

            if (fromKeyWalletAccount != null) {
                if (fromWallet.getBalance().compareTo(finalDeduction) < 0) {
                    return Error.createResponse("Insufficient balance", HttpStatus.BAD_REQUEST,
                            "Your account balance is low.");
                } else {
                    fromWallet.setBalance(fromWallet.getBalance().subtract(finalDeduction));
                    toWallet.setBalance(toWallet.getBalance().add(request.getAmount()));

                    walletRepository.save(fromWallet);
                    walletRepository.save(toWallet);
                }

                String formattedTransactionType = Refactory
                        .formatEnumValue(TransactionType.WALLET_TO_WALLET_TRANSFER);

                Long fromTransactionUniqueId = keyCollections.createSnowflakeUniqueId();
                Long toTransactionUniqueId = keyCollections.createSnowflakeUniqueId();

                String FromsessionId = keyCollections.generateSessionId();

                WalletTransactionHistory fromTransaction = new WalletTransactionHistory();
                fromTransaction.setId(fromTransactionUniqueId);
                fromTransaction.setWallet(fromWallet);
                fromTransaction.setSessionId(FromsessionId);
                fromTransaction.setAmount(request.getAmount().negate());
                fromTransaction.setType(TransactionType.DEBITED);
                fromTransaction.setMessage("TRANSFERRED " + request.getAmount() + " TO " + toUser.getUsername());
                fromTransaction.setStatus(TransactionStatus.Success.toString());
                fromTransaction.setDescription(formattedTransactionType);
                walletTransanctionHistoryRepository.save(fromTransaction);

                String TosessionId = keyCollections.generateSessionId();

                WalletTransactionHistory toTransaction = new WalletTransactionHistory();
                toTransaction.setId(toTransactionUniqueId);
                toTransaction.setWallet(toWallet);
                toTransaction.setSessionId(TosessionId);
                toTransaction.setAmount(request.getAmount());
                toTransaction.setMessage("TRANSFER FROM " + fromUser.getUsername() + " TO " + toUser.getUsername());
                toTransaction.setStatus(TransactionStatus.Success.toString());
                toTransaction.setType(TransactionType.CREDITED);
                toTransaction.setDescription(formattedTransactionType);
                walletTransanctionHistoryRepository.save(toTransaction);

                // process platform fees deductions
                addRevenue(feeAmount);

                Wallet SenderTotalBalance = getUserWalletByUserWalletId(fromUser.getId());
                Wallet RecipientTotalBalance = getUserWalletByUserWalletId(toUser.getId());

                String senderEmail = fromUser.getEmail();
                String recipientEmail = toUser.getEmail();

                BigDecimal transferAmount = request.getAmount();

                // notification
                String senderFullName = fromUser.getRecords().get(0).getFirstName() +" "+ fromUser.getRecords().get(0).getLastName();

                String receiverFullName = toUser.getRecords().get(0).getFirstName() +" "+ toUser.getRecords().get(0).getLastName();
                
                // The Recipient receive credit alert notification message
                CompletableFuture.runAsync(() -> walletNotificationProducer.sendCreditWalletNotification(recipientEmail, transferAmount, senderFullName, receiverFullName, RecipientTotalBalance.getBalance())).exceptionally(ex -> handleAsyncError("Failed to send transaction notification to sender email.", ex));
                // The sender receive debit alert notification message
                CompletableFuture.runAsync(() -> walletNotificationProducer.sendDebitWalletNotification(senderEmail, feeAmount, transferAmount, senderFullName, receiverFullName, SenderTotalBalance.getBalance())).exceptionally(ex -> handleAsyncError("Failed to send transaction notification to sender email.", ex));

            }
            
            return Error.createResponse("Transaction successful", HttpStatus.OK,
                    request.getAmount() + " has been successfully sent to " + request.getToUser());
        } catch (UserNotFoundException ex) {
            // Handle user not found error and return a specific response
            return Error.createResponse(
                    "User not found",
                    HttpStatus.BAD_REQUEST,
                    ex.getDetails() 
            );
        } catch (Exception ex) {
                // Handle other unexpected errors 
                return Error.createResponse(
                        "An unexpected error occurred.",
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        ex.getMessage());
            }
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

        // get platform fee amount
        BigDecimal feePercentage = getFeePercentage();
        // Calculate the fee based on the transfer amount
        BigDecimal feeAmount = request.getAmount().multiply(feePercentage);
        // Calculate the total deduction (amount + fee)
        BigDecimal finalDeduction = feeAmount.add(request.getAmount());

        if (fromKeyWalletAccount != null) {
            if (fromKeyWalletAccount.getBalance().compareTo(finalDeduction) < 0) {
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

    public void addRevenue(BigDecimal feeAmount) {
        Revenue existingRevenue = revenueRepository.findFirstByOrderById();
        
        if (existingRevenue != null) {
            existingRevenue.setBalance(existingRevenue.getBalance().add(feeAmount));
            existingRevenue.setUpdatedAt(LocalDateTime.now());
            revenueRepository.save(existingRevenue);
        } else {
            // If no revenue record exists, create a new one
            Revenue newRevenue = new Revenue();
            newRevenue.setId(keyCollections.createSnowflakeUniqueId());
            newRevenue.setBalance(feeAmount);
            newRevenue.setUpdatedAt(LocalDateTime.now());
            revenueRepository.save(newRevenue);
        }
    }

    private BigDecimal getFeePercentage() {
        TransactionFee feeSettings = transactionFeeRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Fee settings not found"));
        return feeSettings.getTakerFeePercentage();
    }

    private Void handleAsyncError(String errorMessage, Throwable ex) {
        System.out.println(errorMessage + ": " + ex.getMessage());
        return null;
    }


} 