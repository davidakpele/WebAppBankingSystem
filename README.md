# Global Banking System Software With Spring Security, Go-lang and Django

Create Secure banking system web application with Spring security, Go-lang and Django, The application contains both mobile money transaction, Bill payment and cryptocurrency trading

# Mobile banking functionalities, Bill Payment and Crypto Trading.

##Created Complete Global banking system with spring boot, spring rest, spring data JDA, spring session and so on.

[![N|Solid](https://cldup.com/dTxpPi9lDf.thumb.png)](https://nodesource.com/products/nsolid)

[![Build Status](https://travis-ci.org/joemccann/dillinger.svg?branch=master)](https://travis-ci.org/joemccann/dillinger)
## Table of Contents
* **Introduction**
* **Quick Start**
* **Authentication**
* **Authorization**
* **User Verification**
* **Two-Factor Authentication**
* **Service Registry**
* **Security Configuration**
* **JWT Configuration**
* **User Wallet Transactions**
* **Crypto Trading**
* **Creating Crypto Order [BUY & SELL] & [Wallet to Wallet trade]**
* **Cryto History log**
* **Wallet History log**
* **Notification handler**
* **File Handling**
* **Message broker with RabbitMq**
* **Email Configuration**
* **Crons schedule for monthly maintaince fees**
* **Payment gateways with Paystack and flutterwave API Integration**
* **Middleware on IpAddress Interceptor [For transaction monitor on things like {High-Volume-Or-Frequent-Transactions, Inconsistent-Behavior, High-Risk-Region, Same-IpAddress-Transaction-5mins-After-Deposit, From-Blacklisted-Address}]**
* **Cors Configuration**
* **Exception Handling**

## Introduction

> Spring boot is an open-source MVC framework built in java. This framework includes robust Rest API, form handling and validation mechanisms, security. I Developed a secure and efficient Global Bank API to facilitate digital transactions and blockchain integration.

> Break down of the application workflow are:

## Goals
- What are goals of the Global banking system?

1. The application contains both mobile money transaction and cryptocurrency trading.
    * To use this application, Users need to sign-up, get verified before sign-in.
    * This application or platform focus on ten major crypto coin these are [bitcoin, solana, ethereum, tether, binancecoin, usd-coin, ripple, staked-ether, dogecoin and tron].
    * Each of these coin are unique and after user authentication, authorization and verification, user will be assign wallet key address to each of these coin to manage his transactions or crypto trade.
    * User will also be given a naira wallet account where he can deposit and withdraw or trade by buying crypto asset with his naira fund.
2. User Wallet or eWallet or You can call it Naira Wallet, this hold local currency data, currency like naira, dollar or euro but in this particalar application i used naira, Hopefully in future we can implement currency swapping or exchange.
    > This ables users to deposit from their bank to the platform wallet.
    > They can withdraw/transfer from their wallet to another user wallet or transfer to bank account.
    > They can view the wallet history [deposit, withdraw, credit],
    > There are many services User can do aside from deposit and withdraw, User can aslo pay bills like:
    * Eletricity bills, 
    * Buy Airtime and Airtime data bundle like [MTN, GLO, ETISALAT, AIRTEL].
    * pay for cable like [DSTV, GO TV, STAR TIME].
    * BUY and SELL GIFT CARD
    * User can Deposit or fund betting wallet by using his betting platform ID.
    * User can withdraw or transfer money to bank like first bank, union bank outside the platform.
    * User can transfer money money to another user in same platform and all he need is username of that user.
    > Users can view service history.
    > For security reasons, use have transfer pin or password to complete this actions above. User have login successfully, Account maybe set to 2FA-Authentication but tranfering or taking money from wallet need additional security which is why i add wallet locked down password.
    > When user deposit on his wallet, notification is sent with rabbitmq broker process for actions and new balance record.
    > When user withdraw or transfer money to another users within the platform, notifications is send to both users emails, sender receieve debit alert notification and new record, and recipient receive credit alert notification wit the new wallet balance record.
    - Middleware on IpAddress Interceptor was also implemented here on wallet transactions such as:,
        * Large Transactions (High Amount)
            > Condition: Any transaction that exceeds a predefined threshold amount set by the platform.
            > Reason: Large transactions can be suspicious, especially if they are inconsistent with the user’s history.
            > Example: A transfer exceeding N25,000,000.00 or the equivalent in cryptocurrency might be flagged for review.
        * Multiple Transactions in a Short Timeframe (High Frequency)
            > Flag if more than 5 transactions in 10 minutes or total amount exceeds threshold".
            > Condition: If a user conducts multiple large transactions within a short period (e.g., several large transfers within an hour or day).
            > Reason: High-frequency transactions, especially large ones, could indicate potential money laundering or illegal activity.
            > Example: More than three transfers exceeding N10,000,000.00 each within a 24-hour period.
        * Transactions Involving High-Risk Regions or Countries
            > Condition: Transactions sent to or received from wallet addresses associated with high-risk regions or countries (e.g., countries under sanctions or with a high risk of money laundering).
            > Reason: To comply with international AML regulations and avoid transactions with sanctioned entities.
            > Example: A user transferring a significant amount of cryptocurrency to a wallet known to belong to a high-risk jurisdiction.
        * Transactions Between Accounts Using the Same IP Address
            > Condition: Multiple accounts executing transactions from the same IP address within a short period.
            > Reason: This could be a sign of suspicious activity such as a single entity controlling multiple accounts (a practice called "sybil attack").
            > Example: Three different accounts transferring funds to one another, all initiated from the same IP address within an hour.
        * Transactions Associated with Newly Created or Unverified Wallets
            > Condition: Transactions to or from wallets that are newly created or have not been verified.
            > Reason: New or unverified wallets may be used for fraudulent activities or scams.
            > Example: A user sending funds to a wallet that was created within the last hour.
        * Deposits Followed by Immediate Transfers
            > A user makes a deposit into their account and immediately transfers the entire amount elsewhere.
            > This behavior could indicate attempts to obfuscate the origin of the funds (layering phase of money laundering).
            > Example: A deposit of N5,000 followed by an immediate transfer of N4,950 within minutes.
        * Transactions From Blacklisted Wallet Addresses:
            > Check if trying to withdraw from blacklist wallet even though, user can not login, but this has to also be check for security reasons.
            > Condition: Transactions involving wallet addresses that are blacklisted or associated with suspicious or illegal activity.
            > Reason: To prevent and trace interactions with entities known for fraud, hacking, or illegal trade.
            > Example: A user receiving funds from a wallet identified as part of a scam operation.
        * InconsistentBehavior:
            > Criteria 1: Check for sudden large transactions compared to the user's typical behavior
            > Threshold: if a transaction is more than 5 times the average amount
            > Check if there are too many transactions in a short period (e.g., > 10 in 24 hours)
            > Check for transactions from different IP addresses (suggesting multiple locations or devices)
            > Check for unusual transaction types (e.g., frequent withdrawals)
3. Crypto Trading.
    * Platform trading: Users can trade crypto with another user in same platform, How it work?
        - Seller user want to sell crypto or bitcoin to another user in same platform, Seller user only need the buyer username, enter amount, select the crypto network like bitcoin or ethereum depending the network buyer depend and in the form amount validate and check coin market value from "Gecko" api service providing the currency seller is checking form, 
            * Seller want to sell bitcoin of 0.10 to buyer in usd or naira, when user enters the amount of coin want to sell, it automatically fetch coin value from Gecko api service with assetid of "btc" and currency of "usd".
        - When the form is submitted, spring boot validate the data payloads first-> check the user is authorized to access that endpoin, second-> check if user exist in the database, third-> check if the user exist check if the user is the actual owner of the wallet he is attempting on making a trade, meaning-- if the bitcoin wallet belongs to the seller. fourth-> check if user have enough balance from the crypto wallet to make that trade, meaning-- check if user bitcoin wallet has 0.10 balance or more to sell to the buy.
        - Second Backend Validation is checking if Buyer has enough naira fund to buy the bitcoin from the seller else inform the buyer that he do not have enough fund to purchase the bitcoin, this email notification does not include the seller, The seller will only be notify that buyer has cancel the order request but if buyer has enough fund then deduct seller bitcoin wallet and credit buyer wallet and deduct the amount from Buyer NAIRA WALLET meaning "BUYER PAYING FOR THE BITCOIN HE BOUGHT" and credit seller naira wallet.  
        - Platform fees: in between that exchange and making successful trade, platform has a service manager responsable for making charges based on the amount or volume of coin seller is seller or buyer is buying, the deduction can be 0.25% or 0.5%.
            * Question is "What are you charge on, Crypto or from naira currency?
            - well in my case and this user to user platform trade, i choose naira or call it local currency, so i charged seller from the buyer payment going to seller naira wallet. 
            > Seller want to sell 0.10 bitcoin to buyer at N5,000 so buyer is going paying seller 5k, platform charge is inside this 5k, example is 0.50% from 5k = N4,975.
    * P2P Trading: This allow users trade directly with each other,
        -- This Aspect involves user listing their assets, meaning Users can create Order without having or knowing actual buyer or sell. example of this is:
        > User can go to p2p trade, see listing orders both buy and sell, though in my frontend, i separate them with mapping, ["All" | "Buy" | "Sell"] this help users to navigate to a specific section can either see all the orders or only buy orders or only sell orders. 
        -- Create this order involve some few steps, the payload look like this example:
```sh
{
  "userId":"1",
  "asset":"bitcoin",
  "type":"SELL",
  "price":"1953.50",
  "amount":"0.10", 
  "filledAmount":"1953.50",
  "status":"OPEN",
  "currency":"ngn",
  "bankId":"3"
}
```
 * Above you see placing sell order payload looks like but note originally the bankId represent the account details buyer will use to make payment, so in most times it usually bankdetails and the payloads look like this below.
 ```sh
{
  "userId":"1",
  "asset":"bitcoin",
  "type":"SELL",
  "price":"1953.50",
  "amount":"0.10", 
  "filled_amount":"1953.50",
  "status":"OPEN",
  "currency":"ngn",
  "bank_details":{
    "account_holder_name":"Peter Donald",
    "account_number":"12345678901",
    "bank_name":"City Bank"
  }
}
```

* But because user have naira wallet and also has add_bank enpoint and ui where user can add their details which is connected to paystack for user bank details verification, meaning user must provide a valid account details but if user hasn't add any bank yet and tries to sell, error will return telling user to provide bankdetails id which means must need to add bank details to this platform before making sell trading. 

* When user submit he post request to create sell order on p2p trade, i create order service matcher, this service takes the user post request and check order table to see if there is any BUY order that matches this SELL order, meaning- check if other users has created any BUY order that matches with "amount, price, asset and currency" if No match then substract the amount of coin bitcon seller bitcoin wallet and save order in order table, set order status to OPEN but if there is any match, it automatically fill the order and sell notifications to both user, for seller, it inform the seller that this order that was created has found buyer match and has also send the buyer payment receipt to complete the payment, the order is also save in order table, status is now "PAYMENT_PENDING" and now escrow service has HOLD that orderId, sellerId and buyerId. when seller receive payment from buyer, it request to update order status from "PAYMENT_PENDING" to "PAYMENT_VERIFIED", when escrow service sees this order is status have be updated then move the crypto coin amount to the buyer wallet or lets say credit the buyer crypto wallet.

### WHAT IS ESCROW?

> An escrow is a financial arrangement where a third party holds and regulates the payment of funds or assets between two transacting parties. It ensures that both the buyer and seller fulfill their obligations before the transaction is completed. Escrow is commonly used in large transactions or scenarios where trust between parties is low, such as real estate deals, online purchases, or certain cryptocurrency transactions.

* So in this crypto trade, escrow service hold the crypto when a seller place sell order with or without a buyer, if no buy yet, buyerid column on that order will be empty till a buyer place order to buy ther asset then update the escrow table "Order row->buyId" so the escrow can use to credit the buyer when seller confirm payment.

* This application has component class that runs every 5 seconds specifically for payment checking, this class check order, escrow table if Seller has updated order status to "PAYMENT_VERIFIED", if any order has been found seller made an update it automatically release the crypto to buyer wallet and also send notification to both users, this service can handle hundreds and thusands of update in a seconds.
-  Why this service?
    > It also the platform admin to reduce stress of monite payment update and releasing assets to users ON TIME. Imagine a human being responsible to manage this task, in a small plat trade yes he can but nobody want to remain small in market, as the number of users increases, this aspect because more difficult for a mediator to manage. platform can have hundreds on order and hundres of updates same time from different users, anything finanical market delivering time space matters, buyers can not wait 24hrs not be credited because platform mediator has many order to confirm before releasing assets. I leverage java strength to build task manager or cros jobs.
    
4. Wallet to wallet trade: This involves trading with user with their wallet key address but in this application only when user want to recieve or sell to EXTERNAL user you need wallet address.
    * If Our user want to sell to external user outside this platform, he needs other user wallet address. this part was NOT FULLY COMPLETED because i needed to integrate Blockchain technology but i stopped at when developer will need to integrate the blockchain and verify success trade and substract amount from  seller wallet.

    > Please noted: if to be complete, please add platform fees service.

5. Notification and History: On every action made, action in ping and history save for local concurrency transaction or cryptocurrency trading, User can fetch or view their histories both combine history [crypto and local currency] or Just local currency Banking history.

6. File Handling, On User profile, user can update profile picture and image path will be save in the database, image will be moved to resourses/static/image/*
    * Note:  When user update profile picture, system rename the image name to userId example is "10121.png" The reason is because i want to want when ever user upload profile image system hold bush of useless images, no problem but i consider them a trash. so if image with this Id name 10121 exist before, remove and replace with the new user profile image and update the database pathurl,
7. Email Configuration: This is want to i use most in this application to handle notifications, from sign-up, to change of password, 2FA-authentication sign-in, forget password, transaction notifications, payment notifications and so on.
8. FlutterWave and Paystack API Integration  where carefully integrated from resourses/application.yml file.
> This help user to verify bank account number, deposit fund to user wallet, withdraw fund from wallet to banks and pay bills.

## Quick Start

1. Clone the repo: git clone **https://github.com/davidakpele/securewallet**


## Folder Structure
```
C:.
├───.mvn
│   └───wrapper
├───src
│   ├───main
│   │   ├───java
│   │   │   └───com
│   │   │       └───iot
│   │   │           └───authentication
│   │   │               ├───blockchain
│   │   │               ├───config
│   │   │               ├───controllers
│   │   │               ├───dto
│   │   │               ├───enum_
│   │   │               ├───exceptions
│   │   │               ├───middleware
│   │   │               ├───models
│   │   │               │   └───inc_
│   │   │               ├───payloads
│   │   │               ├───properties
│   │   │               ├───repository
│   │   │               ├───responses
│   │   │               ├───services
│   │   │               ├───ServicesImplementations
│   │   │               └───util
│   │   └───resources
│   │       ├───META-INF
│   │       ├───static
│   │       │   └───image
│   │       └───templates
│   └───test
│       └───java
│           └───com
│               └───iot
│                   └───authentication
│                       └───scheduler
└───target
    ├───classes
    │   ├───com
    │   │   └───iot
    │   │       └───authentication
    │   │           ├───blockchain
    │   │           ├───config
    │   │           ├───controllers
    │   │           ├───dto
    │   │           ├───enum_
    │   │           ├───exceptions
    │   │           ├───middleware
    │   │           ├───models
    │   │           │   └───inc_
    │   │           ├───payloads
    │   │           ├───properties
    │   │           ├───repository
    │   │           ├───responses
    │   │           ├───services
    │   │           ├───ServicesImplementations
    │   │           └───util
    │   ├───META-INF
    │   ├───static
    │   │   └───image
    │   └───templates
    ├───generated-sources
    │   └───annotations
    ├───generated-test-sources
    │   └───test-annotations
    ├───maven-archiver
    ├───maven-status
    │   └───maven-compiler-plugin
    │       ├───compile
    │       │   └───default-compile
    │       └───testCompile
    │           └───default-testCompile
    ├───surefire-reports
    └───test-classes
        └───com
            └───iot
                └───authentication
                    └───scheduler
```
## Controllers
**WalletController.java**
```java

    // create wallet deposit 
    @PostMapping("/create/deposit")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createDeposit(@RequestBody CreateTransactionRequest request,
            Authentication authentication) {
        if (request.getType().equals(TransactionType.DEPOSIT)) {
            return depositService.createDeposit(request, authentication);
        }
        return Error.createResponse("Wrong Transaction format.", HttpStatus.BAD_REQUEST,
                "please change the transaction Type to Deposit");
    }

    // transfer to another user within the platform using their username
    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> makeTransferWithUsername(@RequestBody TransactionRequest request, Authentication authentication, HttpServletRequest httpRequest) {
        String username = authentication.getName();
        if (request.getFromUser().isEmpty()) {
            return Error.createResponse("Sender id is require.", HttpStatus.BAD_REQUEST,
                    "Please provide the sender user id.");
        }
        if (request.getToUser().isEmpty()) {
            return Error.createResponse("Recipient id is require.", HttpStatus.BAD_REQUEST,
                    "Please provide the Recipient user id.");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Error.createResponse("Amount is required and must be greater than zero.", HttpStatus.BAD_REQUEST,
                    "Please provide a valid amount you want to transfer to this user.");
        }

        String providedPin = request.getTransferpin();

        if (providedPin == null || providedPin.isEmpty()) {
            return Error.createResponse("Transfer pin is required.", HttpStatus.BAD_REQUEST,
                    "Please provide your transfer pin.");
        }
        return walletService.makeTransferWithUsername(username, request, authentication, httpRequest);
    }
```

## Service
**WalletService.java**
```java
// service
@Service
public class WalletServiceImplementation implements WalletService {
    @Override
    public void deposit(Long userId, BigDecimal amount) {
        Optional<Wallet> walletOptional = getWalletByUserId(userId);

        if (walletOptional.isPresent()) {
            Wallet wallet = walletOptional.get();
            wallet.setBalance(wallet.getBalance().add(amount));
            walletRepository.save(wallet);

            WalletTransactionHistory history = new WalletTransactionHistory();
            history.setWallet(wallet);
            history.setAmount(amount);
            history.setType(TransactionType.DEPOSIT);
            walletTransanctionHistoryRepository.save(history);

            Optional<Users> fromUser = userRepository.findById(userId);

            Wallet SenderTotalBalance = getUserWalletByUserWalletId(walletOptional.get().getId());
 
            String senderEmail = fromUser.get().getEmail();
            LocalDateTime transactionTime  = LocalDateTime.now();
            Optional<UserRecord> senderName = userRecordImplementation.getUserNames(fromUser.get().getId());

            // notification
            String senderFullName = senderName.get().getFirstName() + " " + senderName.get().getLastName();
            CompletableFuture.runAsync(() -> emailServiceImplementation.sendDepositNotification(senderEmail,
                            senderFullName, amount, transactionTime, SenderTotalBalance.getBalance())).exceptionally(ex -> handleAsyncError("Failed to send transaction notification to sender email.", ex));
            
        } else {
            throw new EntityNotFoundException("Wallet not found for userId: " + userId);
        }
    }

    // transfer within same platform
      @Override
    public ResponseEntity<?> makeTransferWithUsername(String username, TransactionRequest request, Authentication authentication, HttpServletRequest httpRequest) {
        Optional<Users> fromUser = userRepository.findByUsername(username);
        
        Optional<Users> toUser = userRepository.findByUsername(request.getToUser());

        String receiverUsername = authentication.getName();
        Users user = usersServices.getUserByUsername(receiverUsername);
        if (!fromUser.isPresent() || !fromUser.get().getUsername().equals(request.getFromUser())) {
            return Error.createResponse("Sorry this user does not exist in our system.",
                    HttpStatus.BAD_REQUEST,
                    "Sender User does not exist, please provide valid username.");
        }
        
        if (user == null) {
            return Error.createResponse("User not found", HttpStatus.NOT_FOUND,
                    "User not found");
        } else if (!fromUser.get().getUsername().equals(user.getUsername())) {
            return Error.createResponse(
                    "Fraudulent action is taken here, You are not the authorized user to operate this wallet.",
                    HttpStatus.FORBIDDEN,
                    "One more attempt from you again, you will be reported to the Economic and Financial Crimes Commission (EFCC).");
        }

        boolean isLockedAccount = userRecordImplementation.isLockedAccount(fromUser.get().getId());
        boolean isBlockedAccount = userRecordImplementation.isBlockedAccount(fromUser.get().getId());

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

        if (!fromUser.isPresent()) {
            return Error.createResponse("Sorry this user does not exist in our system.",
                    HttpStatus.BAD_REQUEST,
                    "Sender User does not exist, please provide valid username.");
        }

        if (!toUser.isPresent()) {
            return Error.createResponse("Sorry " + request.getToUser() + " user does not exist in our system.",
                    HttpStatus.BAD_REQUEST,
                    "Receiver User does not exist, please provide valid username.");
        }

        Wallet fromKeyWalletAccount = getUserWalletByUserWalletId(fromUser.get().getId());
        Wallet toKeyWalletAccount = getUserWalletByUserWalletId(toUser.get().getId());

        // Check for suspicious behaviors
        if (transactionMonitor.isHighVolumeOrFrequentTransactions(user.getId())) {
            return Error.createResponse("Account temporarily banned due to high volume of transactions.", HttpStatus.FORBIDDEN,
                    "Please contact support.");
        }

        if (transactionMonitor.isInconsistentBehavior(user.getId())) {
            return Error.createResponse("Account temporarily banned due to inconsistent behavior.", HttpStatus.FORBIDDEN,
                    "Please contact support.");
        }

        if (transactionMonitor.isHighRiskRegion(request)) {
            return Error.createResponse("Account temporarily banned due to transactions from high-risk regions.", HttpStatus.FORBIDDEN,
                    "Please contact support.");
        }

        if (transactionMonitor.isSameIpAddressTransaction(fromUser.get().getId(), toUser.get().getId(), httpRequest.getRemoteAddr())) {
            return Error.createResponse("Account temporarily banned due to transactions from the same IP address.", HttpStatus.FORBIDDEN,
                    "Please contact support.");
        }

        if (transactionMonitor.isUnverifiedOrNewWallet(user.getId())) {
            return Error.createResponse("Account temporarily banned due to unverified or newly created wallet.", HttpStatus.FORBIDDEN,
                    "Please contact support.");
        }

        if (transactionMonitor.isImmediateTransferAfterDeposit(user.getId())) {
            return Error.createResponse("Account temporarily banned due to immediate transfers following deposits.", HttpStatus.FORBIDDEN,
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
        // if user tries to move money from wallet to same wallet.
        if (fromUser.get().getUsername().equals(request.getToUser())) {
            return Error.createResponse("Sorry, You request is not acceptable in our platform. You are making a requet to send "+request.getAmount()+" to you own very wallet that belongs to your.", HttpStatus.BAD_REQUEST,
                    "You can send money from you to wallet to your wallet.");
        }
        
        if (!passwordEncoder.matches(providedPin, fromKeyWalletAccount.getPassword())) {
            return Error.createResponse("Invalid transfer pin.", HttpStatus.UNAUTHORIZED,
                    "The provided transfer pin is incorrect.");
        }

        String ipAddress = httpRequest.getRemoteAddr();

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
            toWallet.setUserId(toUser.get().getId());
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
            String formattedTransactionType = EnumHelper.formatEnumValue(TransactionType.WALLET_TO_WALLET_TRANSFER);

            Long fromTransactionUniqueId = keysWrapper.createSnowflakeUniqueId();
            Long toTransactionUniqueId = keysWrapper.createSnowflakeUniqueId();

            String FromsessionId = keysWrapper.generateSessionId();

            WalletTransactionHistory fromTransaction = new WalletTransactionHistory();
            fromTransaction.setId(fromTransactionUniqueId);
            fromTransaction.setWallet(fromWallet);
            fromTransaction.setSessionId(FromsessionId); 
            fromTransaction.setAmount(request.getAmount().negate());
            fromTransaction.setType(TransactionType.DEBITED);
            fromTransaction.setMessage("TRANSFERRED " + request.getAmount() + " TO " + toUser.get().getUsername());
            fromTransaction.setStatus(TransactionStatus.Success.toString());
            fromTransaction.setIpAddress(ipAddress);
            fromTransaction.setDescription(formattedTransactionType);
            walletTransanctionHistoryRepository.save(fromTransaction);

            String TosessionId = keysWrapper.generateSessionId(); 

            WalletTransactionHistory toTransaction = new WalletTransactionHistory();
            toTransaction.setId(toTransactionUniqueId);
            toTransaction.setWallet(toWallet);
            toTransaction.setSessionId(TosessionId); 
            toTransaction.setAmount(request.getAmount());
            toTransaction.setMessage("TRANSFER FROM " + fromUser.get().getUsername() + " TO " + toUser.get().getUsername());
            toTransaction.setStatus(TransactionStatus.Success.toString());
            toTransaction.setType(TransactionType.CREDITED);
            toTransaction.setIpAddress(ipAddress);
            toTransaction.setDescription(formattedTransactionType);
            walletTransanctionHistoryRepository.save(toTransaction);

            // process platform fees deductions
            addRevenue(feeAmount);

            Wallet SenderTotalBalance = getUserWalletByUserWalletId(fromUser.get().getId());
            Wallet RecipientTotalBalance = getUserWalletByUserWalletId(toUser.get().getId());

            String senderEmail = fromUser.get().getEmail();
            String recipientEmail = toUser.get().getEmail();

            BigDecimal transferAmount = request.getAmount();

            Optional<UserRecord> senderName = userRecordImplementation.getUserNames(fromUser.get().getId());

            Optional<UserRecord> recipientName = userRecordImplementation.getUserNames(toUser.get().getId());
            // notification
            String senderFullName = senderName.get().getFirstName() + " " + senderName.get().getLastName();

            String receiverFullName = recipientName.get().getFirstName() + " " + senderName.get().getLastName();

            CompletableFuture.runAsync(() ->emailServiceImplementation.sendCreditWalletNotificationToRecipient(recipientEmail, transferAmount,
                    senderFullName, receiverFullName, RecipientTotalBalance.getBalance()))
                    .exceptionally(ex -> handleAsyncError("Failed to send transaction notification to sender email.", ex));
            CompletableFuture.runAsync(() ->  emailServiceImplementation.sendDebitWalletNotificationToRecipient(senderEmail, feeAmount, transferAmount,
                    senderFullName, receiverFullName, SenderTotalBalance.getBalance()))
                    .exceptionally(ex -> handleAsyncError("Failed to send transaction notification to recipient email.", ex));
        }
        return Error.createResponse("Transaction successful", HttpStatus.OK,
                request.getAmount() + " has been successfully sent to " + request.getToUser());
    }


}
```	
