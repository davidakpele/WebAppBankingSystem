# Global Banking System Software With Spring Security, Go-lang and Django

Create Secure banking system web application with Spring security, Go-lang and Django, The application contains both mobile money transaction, Bill payment and cryptocurrency trading.

### This is a prototype of the application
> Developed api endpoints to manage the users Global Digital Payment System and crypto trading, supporting both local currency operations and blockchain integration for cryptocurrency trading. Implemented features for platform and P2P crypto trading, integrating blockchain technology with the potential for future currency exchange functionalities.
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
> Skills Demonstrated:
   - Spring Security [Authentication, Authorization and Verification]
   - Go Lang [WebSocket Crypto Trading]
   - Python [Django for Admin]
   - Database [PostgreSQL, MySQL & MongoDB]
   - Payment gateway [Stripe API & Flutter API],
   - Docker [Containerization]
   - RabbitMQ [MQTT & AMQP]
   - Delopment: [AWS].
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
  "userId":"27920132732",
  "asset":"bitcoin",
  "type":"SELL",
  "price":"1953.50",
  "amount":"10.10", 
  "filledAmount":"19753.50",
  "status":"OPEN",
  "currency":"ngn",
  "bankId":"3"
}
```
 * Above you see placing sell order payload looks like but note originally the bankId represent the account details buyer will use to make payment, so in most times it usually bankdetails and the payloads look like this below.
 ```sh
{
  "userId":"27920132732",
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
