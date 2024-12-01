package services

import (
	"fmt"
	"sync"
	"wallet-app/models"
	"wallet-app/payloads"
	"wallet-app/repository"
	"wallet-app/responses"
	"wallet-app/utils"
)

type WalletService interface {
	CreateWalletsForUser(userID uint) error
	GetWalletBalance(userID uint) (map[string]interface{}, error)
	SellWithAddress(request payloads.BuySellRequest, token string) (*responses.TransactionResult, error)
}

type walletService struct {
	walletRepo repository.WalletRepository
	jwtSecret  string
}

func NewWalletService(walletRepo repository.WalletRepository, jwtSecret string) WalletService {
	return &walletService{
		walletRepo: walletRepo,
		jwtSecret:  jwtSecret,
	}
}

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
                Balance:     0.0,
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


func (s *walletService) GetWalletBalance(userID uint) (map[string]interface{}, error) {
	// Fetch all wallets for the given user ID
	wallets, err := s.walletRepo.FindByUserID(userID)
	if err != nil {
		return nil, err
	}

	// Calculate the total balance for the user
	totalBalance := 0.0
	for _, wallet := range wallets {
		totalBalance += wallet.Balance
	}

	// Response preparation
	return map[string]interface{}{
		"coin_wallets":   wallets,
	}, nil
}


func (ws *walletService) SellWithAddress(request payloads.BuySellRequest, token string) (*responses.TransactionResult, error) {
    // Example logic for the "SellWithAddress" method
    // You can add the logic to process the request and return the transaction result

    // For illustration, we are returning a dummy response
    transactionResult := &responses.TransactionResult{
        TransactionID: "12345", // Dummy transaction ID
        Status:        "Success",
        Amount:        request.Amount, // Assuming request.Amount is the amount being sold
    }

    // Logic to process the request goes here...
    return transactionResult, nil
}
