package services

import (
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"strings"
	"sync"
	"wallet-app/models"
	"wallet-app/payloads"
	"wallet-app/repository"
	"wallet-app/responses"
	"wallet-app/utils"
	"github.com/shopspring/decimal"
)

type WalletService interface {
    CreditToBuyer(buyerID uint, amount decimal.Decimal, asset string) error
    CreditToRecipient(receiverID uint, amount decimal.Decimal, address string) error
    DeductFromSender(senderID uint, amount decimal.Decimal, asset string) error
	CreateWalletsForUser(userID uint) error
	GetWalletBalance(userID uint) (map[string]interface{}, error)
	SellWithAddress(request *payloads.BuySellRequest, r *http.Request) (*responses.SuccessResponse, error)
    GetUserWalletAddressKey(serviceType string, userId uint) (*models.Wallet, error)
    UpdateFillAmount(userID uint, cryptoID string, amount decimal.Decimal) error 
}

// walletService struct
type walletService struct {
    walletRepo        repository.WalletRepository
    jwtSecret         string
    userService       UserAPIService
    historyService    *HistoryService
    revenueService    *RevenueService
}

// NewWalletService constructor
func NewWalletService(walletRepo repository.WalletRepository, jwtSecret string, userService UserAPIService, historyService *HistoryService, revenueService *RevenueService) WalletService {
    return &walletService{
        walletRepo:        walletRepo,
        jwtSecret:         jwtSecret,
        userService:       userService,
        historyService:    historyService,  // Injecting history service
        revenueService:    revenueService,
    }
}

// Create wallet for user during account verification
func (s *walletService) CreateWalletsForUser(userID uint) error {
    cryptocurrencies := []string{"Bitcoin", "Ethereum", "Solana", "Binance", "Litecoin", "Shiba Inu", "Dogecoin", "Tron", "Cardano", "Dash", "Ripple", "Polygon"}

    // Check if wallets already exist for the user
    walletsExist, err := s.walletRepo.WalletsExist(userID, cryptocurrencies)
    if err != nil {
        return fmt.Errorf("failed to check if wallets exist for user ID %d: %w", userID, err)
    }

    if walletsExist {
        return fmt.Errorf("wallets already exist for user ID %d", userID)
    }

    // Channel to collect wallet addresses and errors
    walletChan := make(chan *models.Wallet, len(cryptocurrencies))
    errChan := make(chan error, len(cryptocurrencies))

  
    /**
    * Using Goroutines for Asynchronous Execution
    * Create wallets concurrently
    * Goroutines in Go allow you to run functions concurrently, which can significantly speed up tasks like address creation when you're handling a high volume of requests.
    */
    var wg sync.WaitGroup
    for _, crypto := range cryptocurrencies {
        wg.Add(1)
        go func(crypto string) {
            defer wg.Done()

            // Create wallet address for the cryptocurrency
            walletAddress, err := utils.CreateWalletAddressForCrypto(crypto)
            if err != nil {
                errChan <- fmt.Errorf("failed to create wallet for %s: %w", crypto, err)
                return
            }

            // Create the wallet model
            wallet := &models.Wallet{
                CryptoID:    crypto,
                UserID:      userID,
                WalletAddress: walletAddress,
                Balance:     decimal.NewFromFloat(0.0),
                FillAmount:     decimal.NewFromFloat(0.0),
            }

            walletChan <- wallet
        }(crypto)
    }

    // Wait for all goroutines to finish
    wg.Wait()

    // Close the channels
    close(walletChan)
    close(errChan)

    // Collect any errors
    if len(errChan) > 0 {
        return fmt.Errorf("address creation failed: %v", <-errChan)
    }

    // Collect wallets from the channel
    var wallets []models.Wallet
    for wallet := range walletChan {
        wallets = append(wallets, *wallet)
    }

    // Save wallets to the database
    if err := s.walletRepo.Create(wallets); err != nil {
        return fmt.Errorf("failed to create wallets in the database: %w", err)
    }

    return nil
}

// Get User balance from all assets
func (s *walletService) GetWalletBalance(userID uint) (map[string]interface{}, error) {
	// Fetch all wallets for the given user ID
	wallets, err := s.walletRepo.FindByUserID(userID)
	if err != nil {
		return nil, err
	}

	// Calculate the total balance for the user
	totalBalance := decimal.NewFromFloat(0.0)
	for _, wallet := range wallets {
		totalBalance = totalBalance.Add(wallet.Balance)
	}

	// Response preparation
	return map[string]interface{}{
		"coin_wallets":   wallets,
	}, nil
}

func (cs *walletService) SellWithAddress(request *payloads.BuySellRequest, r *http.Request) (*responses.SuccessResponse, error) {
    // Step 1: Extract token
    tokenString, err := cs.ExtractTokenFromHeaders(r)
    if err != nil {
        return nil, responses.CreateErrorResponse("FORBIDDEN ACCESS", "Invalid token: "+err.Error())
    }

    // Step 2: Fetch seller and wallet
    fromUser, err := cs.userService.FindByUsername(request.SellerId)
    if err != nil {
        return nil, responses.CreateErrorResponse("User not found", err.Error())
    }
    fromWallet, err := cs.walletRepo.FindByUserIdAndAsset(uint(fromUser.ID), request.Asset)
    if err != nil {
        return nil, responses.CreateErrorResponse("Seller's wallet not found", err.Error())
    }

    // Step 3: Validate seller's balance
    amountDecimal := request.Amount
    
    if fromWallet.Balance.Cmp(amountDecimal) < 0 {
       return nil, responses.CreateErrorResponse("The seller's wallet balance is not sufficient to cover this transaction.", "Insufficient balance",)
    }

    // Step 4: Fetch buyer wallet
    toWallet, err := cs.walletRepo.FindByWalletAddress(request.RecipientWalletAddress)
    if err != nil {
        return nil, responses.CreateErrorResponse("Buyer wallet not found", err.Error())
    }

    // Step 5: Calculate fees and final deduction

    amount := request.Amount 

    pricePerUnit := request.Price

    feePercentage := decimal.NewFromFloat(0.008)

    // Calculate trade amount
	tradeAmount := amount.Mul(pricePerUnit)

    // Calculate platform fee
	platformFee := tradeAmount.Mul(feePercentage)
    // Step 6: fetch buyer balance from his specific currency wallet
    fetchBuyerBalance, err := cs.userService.FetchBalanceByCurrency(int64(toWallet.UserID), request.Currency, tokenString)
    if err != nil {
        return nil, responses.CreateErrorResponse("Error fetching balance", err.Error())
    }
    
    balanceDecimal := fetchBuyerBalance.Balance  
    
    // Compare with finalDeduction
    if balanceDecimal.Cmp(tradeAmount) < 0 {
        return nil, responses.CreateErrorResponse("Buyer's wallet balance is not sufficient. funds to cover this trade.", "Insufficient funds")
    }

    // Step 7: Update balances and save history
    go func() {
        // 7a action-> Credit Seller Specific Wallet 
        if err := cs.userService.CreditSellerCurrencyWallet(uint(fromUser.ID), uint(toWallet.UserID), request.Currency, request.Price, platformFee, tokenString); err != nil {
            responses.LogError("Failed to credit Seller's "+ request.Currency+" Wallet.", err)
            return
        }
        // 7b action-> Deduct from sender's wallet
        if err := cs.walletRepo.DeductFromWallet(fromUser.ID, request.Asset, request.Amount); err != nil {
            responses.LogError("Failed to deduct balance from sender's wallet", err)
            return
        } 
        
        // 7c action-> Credit recipient's wallet
        if err := cs.walletRepo.CreditToWallet(int64(toWallet.UserID), request.Asset, request.Amount); err != nil {
            responses.LogError("Failed to credit recipient's wallet", err)
            return
        }
        
        sellerFee :=  decimal.NewFromFloat(0.00)
        // 7d action-> Save transaction history with decimal.Decimal values
        SaveTransactionHistory(cs.historyService, uint(fromUser.ID), request.Asset, models.PaymentMethodType(request.PaymentMethod), request.Amount, request.Price, sellerFee, "SELL")
        SaveTransactionHistory(cs.historyService, uint(toWallet.UserID), request.Asset, models.PaymentMethodType(request.PaymentMethod), request.Amount, request.Price, platformFee, "BUY")
        
        // 7e action-> Update Seller FiilAmount
        if err := cs.UpdateFillAmount(uint(fromUser.ID), request.Asset, amount); err != nil {
            responses.LogError("Failed to Update Seller FillAmount", err)
            return
        }
    }()

    // Step 8: Create success response
    successResponse := responses.CreateSuccessResponse(
        "Transaction Successful",
        "The transaction has been completed successfully.",
        map[string]interface{}{
            "Asset": request.Asset,
            "Amount": request.Amount,
            "Price Paid": request.Price,
            "Location": "_SECRET_",
            "Order Status": "SUCCESS",
            "Order Status Reason": "Seller Waiting to Conform Payment",
        },
    )
    return &successResponse, nil
}

// User Fetching Wallet Address Key
func (s *walletService) GetUserWalletAddressKey(serviceType string, userId uint) (*models.Wallet, error) {
    wallet, err := s.walletRepo.FindByUserIdAndAsset(userId, serviceType)
    if err != nil {
        return nil, fmt.Errorf("failed to find wallet: %w", err)
    }
    
    if wallet == nil {
        return nil, fmt.Errorf("wallet not found for user: %d, crypto: %s", userId, serviceType)
    }

    return wallet, nil
}

// Deduct from Sender wallet when selling or sending to another user
func (s *walletService) DeductFromSender(senderID uint, amount decimal.Decimal, asset string) error {
    fromWallet, err := s.walletRepo.FindByUserIdAndAsset(senderID, asset)
    if err != nil {
        return fmt.Errorf("failed to find sender's wallet: %w", err)
    }
    
    if fromWallet == nil {
        return fmt.Errorf("sender's wallet not found for user: %d, asset: %s", senderID, asset)
    }

    // Ensure the sender has enough balance
    if fromWallet.Balance.Cmp(amount) < 0 {
        return fmt.Errorf("insufficient balance in sender's wallet")
    }

    // Deduct from the sender's wallet balance
    fromWallet.Balance = fromWallet.Balance.Sub(amount)

    // Save the updated wallet
    if err := s.walletRepo.Save(fromWallet); err != nil {
        return fmt.Errorf("failed to save sender's wallet: %w", err)
    }

    return nil
}

// Crediting User Buyer / Recipient from Seller wallet
func (s *walletService) CreditToRecipient(receiverID uint, amount decimal.Decimal, address string) error {
    if amount.Cmp(decimal.NewFromFloat(0)) <= 0 {
        return fmt.Errorf("amount must be positive")
    }

    // Find the recipient's wallet by address
    toWallet, err := s.walletRepo.FindByWalletAddress(address)
    if err != nil {
        return fmt.Errorf("failed to find recipient's wallet: %w", err)
    }

    if toWallet == nil {
        return fmt.Errorf("recipient's wallet not found for address: %s", address)
    }

    // Credit the amount to the recipient's wallet
    toWallet.Balance = toWallet.Balance.Add(amount)

    // Save the updated wallet
    if err := s.walletRepo.Save(toWallet); err != nil {
        return fmt.Errorf("failed to save recipient's wallet: %w", err)
    }

    return nil
}

func (s *walletService) CreditToBuyer(buyerID uint, amount decimal.Decimal, asset string) error {
    if amount.Cmp(decimal.NewFromFloat(0)) <= 0 {
        return fmt.Errorf("amount must be positive")
    }

    // Find the buyer's wallet by user ID and asset
    toWallet, err := s.walletRepo.FindByUserIdAndAsset(buyerID, asset)
    if err != nil {
        return fmt.Errorf("failed to find buyer's wallet: %w", err)
    }

    if toWallet == nil {
        return fmt.Errorf("buyer’s wallet not found for user: %d, asset: %s", buyerID, asset)
    }

    // Credit the amount to the buyer's wallet
    toWallet.Balance = toWallet.Balance.Add(amount)

    // Save the updated wallet
    if err := s.walletRepo.Save(toWallet); err != nil {
        return fmt.Errorf("failed to save buyer's wallet: %w", err)
    }

    return nil
}

// FetchWalletBalanceByUserId fetches the wallet balance for a user using their ID
func FetchWalletBalanceByUserId(userId uint) (decimal.Decimal, error) {
	// Define the e-wallet API endpoint
	apiURL := "http://localhost:8081/api/v1/wallet/balance"

	// Build the request URL
	url := fmt.Sprintf("%s?userId=%d", apiURL, userId)

	// Make the HTTP GET request
	resp, err := http.Get(url)
	if err != nil {
		return decimal.Zero, fmt.Errorf("failed to call wallet balance API: %w", err)
	}
	defer resp.Body.Close()

	// Check if the response status is not OK
	if resp.StatusCode != http.StatusOK {
		return decimal.Zero, fmt.Errorf("unexpected response status: %d", resp.StatusCode)
	}

	// Decode the response into WalletBalanceResponse
	var walletResponse WalletBalanceResponse
	if err := json.NewDecoder(resp.Body).Decode(&walletResponse); err != nil {
		return decimal.Zero, fmt.Errorf("failed to decode wallet balance response: %w", err)
	}

	// Convert the central balance to decimal.Decimal
	balance, err := decimal.NewFromString(walletResponse.CentralBalance)
	if err != nil {
		return decimal.Zero, fmt.Errorf("failed to parse central balance: %w", err)
	}

	return balance, nil
}

// DeductFromSender deducts the specified amount from the sender's wallet
func DeductFromSender(walletRepo repository.WalletRepository, senderWalletID uint, amount decimal.Decimal, asset string) error {
	// Use walletRepo to find the wallet by senderWalletID and asset
	senderWallet, err := walletRepo.FindByUserIdAndAsset(senderWalletID, asset)
	if err != nil {
        // Return error if wallet not found
		return err 
	}

	// Check if the sender has enough balance
	if senderWallet.Balance.Cmp(amount) < 0 {  
        // Cmp returns -1, 0, 1 for <, ==, >
		return fmt.Errorf("Insufficient balance in sender's wallet")
	}

	// Deduct the amount from sender's wallet
	senderWallet.Balance = senderWallet.Balance.Sub(amount)

	// Update the wallet in the database
	err = walletRepo.Update(senderWallet)
	if err != nil {
        // Return error if update fails
		return err 
	}

	return nil
}

// CreditToBuyer credits the specified amount to the buyer's wallet
func CreditToBuyer(walletRepo repository.WalletRepository, buyerWalletID uint, amount decimal.Decimal, asset string) error {
    // Find the buyer's wallet by ID and asset
    buyerWallet, err := walletRepo.FindByUserIdAndAsset(buyerWalletID, asset)
    if err != nil {
        return err // Return error if wallet not found
    }

    // Add the amount to the buyer's wallet balance
    buyerWallet.Balance = buyerWallet.Balance.Add(amount)

    // Update the buyer's wallet in the database
    err = walletRepo.Update(buyerWallet)
    if err != nil {
        return err // Return error if update fails
    }

    return nil
}

// Save history
func SaveTransactionHistory(historyService *HistoryService, userID uint, coinID string, paymentMethod models.PaymentMethodType, quantity, price, feeAmount decimal.Decimal, transactionType string) error {
    // Convert transactionType to the appropriate enum type
    var txnType models.TransactionType
    switch transactionType {
    case "BUY":
        txnType = models.TransactionTypeBuy
    case "SELL":
        txnType = models.TransactionTypeSell
    case "SEND":
        txnType = models.TransactionTypeSend
    case "RECEIVE":
        txnType = models.TransactionTypeReceive
    default:
        return fmt.Errorf("invalid transaction type: %s", transactionType)
    }

    // Save transaction history using the HistoryService
    err := historyService.SaveTransactionHistory(
        userID,
        coinID,
        paymentMethod,
        quantity,
        price,
        feeAmount,
        txnType,
    )
    if err != nil {
        return fmt.Errorf("failed to save transaction history for user %d: %w", userID, err)
    }
    return nil
}

// Extract jwt token from header request
func (ws *walletService) ExtractTokenFromHeaders(r *http.Request) (string, error) {
    // Get the Authorization header
    authHeader := r.Header.Get("Authorization")
    if authHeader == "" {
        return "", errors.New("authorization header is missing")
    }

    // Split the header value into "Bearer" and the token
    parts := strings.Split(authHeader, " ")
    if len(parts) != 2 || strings.ToLower(parts[0]) != "bearer" {
        return "", errors.New("invalid authorization header format")
    }

    token := parts[1]
    return token, nil
}

// Update User FillAmount 
func (s *walletService)  UpdateFillAmount(userID uint, cryptoID string, amount decimal.Decimal) error {
    if amount.Cmp(decimal.NewFromFloat(0)) <= 0 {
        return fmt.Errorf("amount must be positive")
    }

    // Find the buyer's wallet by user ID and asset
    toWallet, err := s.walletRepo.FindByUserIdAndAsset(userID, cryptoID)
    if err != nil {
        return fmt.Errorf("failed to find buyer's wallet: %w", err)
    }

    if toWallet == nil {
        return fmt.Errorf("buyer’s wallet not found for user: %d, asset: %s", userID, cryptoID)
    }

    // Credit the amount to the buyer's wallet
    toWallet.FillAmount = toWallet.FillAmount.Add(amount)

    // Save the updated wallet
    if err := s.walletRepo.UpdateFillAmount(toWallet); err != nil {
        return fmt.Errorf("failed to update fillAmount: %w", err)
    }

    return nil

}