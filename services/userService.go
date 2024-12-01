package services

import (
	"errors"
	"math/big"
	"wallet-app/models"
	"wallet-app/payloads"
	"wallet-app/repository"
)

type UserService interface {
    GetUserByUsername(username string) (*models.User, error)
    SellCryptoWithAddress(user *models.User, request *payloads.BuySellRequest) error
}

type UserServiceImpl struct {
    userRepository    repository.UserRepository
    walletRepository  repository.WalletRepository
    revenueService    repository.RevenueService
}

func NewUserService(userRepo repository.UserRepository, walletRepo repository.WalletRepository, revenueSvc repository.RevenueService) UserService {
    return &UserServiceImpl{
        userRepository:   userRepo,
        walletRepository: walletRepo,
        revenueService:   revenueSvc,
    }
}

// GetUserByUsername fetches a user by their username.
func (s *UserServiceImpl) GetUserByUsername(username string) (*models.User, error) {
    return s.userRepository.FindByUsername(username)
}

// SellCryptoWithAddress handles the sale operation between two wallets.
func (s *UserServiceImpl) SellCryptoWithAddress(user *models.User, request *payloads.BuySellRequest) error {
    // Find the sender's wallet
    fromWallet, err := s.walletRepository.FindByUserIdAndCryptoId(user.ID, request.Asset)
    if err != nil {
        return errors.New("sender's wallet not found")
    }

    // Convert wallet balance to big.Float for high precision calculations
    walletBalance := new(big.Float).SetFloat64(fromWallet.Balance) 

    // Find the recipient's wallet by their wallet address
    toWallet, err := s.walletRepository.FindByWalletAddress(request.RecipientWalletAddress)
    if err != nil {
        return errors.New("recipient's wallet not found")
    }

    // Check if the sender has sufficient balance
    if walletBalance.Cmp(request.Amount) < 0 { // Compare wallet balance with the request amount
        return errors.New("insufficient balance to complete the transaction")
    }

    // Calculate the transaction fee
    feePercentage := s.revenueService.GetFeePercentage() // This returns a float64
    feePercentageBig := new(big.Float).SetFloat64(feePercentage) // Convert to *big.Float
    feeAmount := new(big.Float).Mul(request.Amount, feePercentageBig) // Use feePercentageBig instead
    netAmount := new(big.Float).Sub(request.Amount, feeAmount)

    // Deduct the total amount (including the fee) from the sender's wallet
    finalDeduction := new(big.Float).Add(feeAmount, request.Amount)
    if err := s.walletRepository.DeductFromWallet(fromWallet, finalDeduction); err != nil {
        return errors.New("failed to deduct amount from sender's wallet")
    }

    // Credit the net amount (after fees) to the recipient's wallet
    if err := s.walletRepository.CreditToWallet(toWallet, netAmount); err != nil {
        return errors.New("failed to credit recipient's wallet")
    }

    // Add the transaction fee to the platform's fee wallet
    if err := s.revenueService.AddToPlatformFeeWallet(feeAmount); err != nil {
        return errors.New("failed to add fee to platform wallet")
    }

    return nil
}

