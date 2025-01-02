package services

import (
	"time"
	"wallet-app/models"
	"wallet-app/repository"

	"github.com/shopspring/decimal"
)

type HistoryService struct {
	historyRepo repository.HistoryRepository
}

// Constructor for HistoryService
func NewHistoryService(historyRepo repository.HistoryRepository) *HistoryService {
	return &HistoryService{
		historyRepo: historyRepo,
	}
}

// SaveTransactionHistory saves a transaction history
func (s *HistoryService) SaveTransactionHistory(userID uint, coinID string, paymentMethod models.PaymentMethodType, quantity, price, fee decimal.Decimal, transactionType models.TransactionType) error {
    history := &models.History{
        Type:     transactionType,
        Quantity: quantity,
        Price:    price,
        Fee:      fee,
        CoinID:   coinID,
        UserID:   userID,
        PaymentMethod: paymentMethod,
        CreatedOn: time.Now(), 
    }
    return s.historyRepo.SaveHistory(history)
}

// FetchTransactionHistoryByUser fetches all transaction histories for a user
func (s *HistoryService) FetchTransactionHistoryByUser(userID uint) ([]models.History, error) {
	return s.historyRepo.FindHistoryByUserId(userID)
}
