package services

import (
	"errors"
	"fmt"
	"wallet-app/models"
	"wallet-app/repository"

	"github.com/shopspring/decimal"
	"gorm.io/gorm"
)

// revenueServiceImpl implements the RevenueService interface
type RevenueService struct {
    RevenueRepo *repository.RevenueRepository
}

// NewRevenueService creates a new instance of RevenueService
func NewRevenueService(revenueRepo *repository.RevenueRepository) *RevenueService {
    return &RevenueService{
        RevenueRepo: revenueRepo,
    }
}


// GetRevenueByID fetches revenue by ID using the RevenueRepository
func (s *RevenueService) GetRevenueByID(id uint) (*models.Revenue, error) {
    return s.RevenueRepo.GetRevenueByID(id)
}

// AddRevenue adds a new revenue record using the RevenueRepository
func (s *RevenueService) AddRevenue(fee decimal.Decimal) error {
    var revenue models.Revenue

    // Check if a revenue record exists
    err := s.RevenueRepo.DB.First(&revenue).Error
    if errors.Is(err, gorm.ErrRecordNotFound) {
        // Create a new revenue record if it does not exist
        revenue = models.Revenue{
            Balance: fee,
        }
        if err := s.RevenueRepo.AddRevenue(&revenue); err != nil {
            return fmt.Errorf("failed to create revenue record: %w", err)
        }
    } else if err != nil {
        return fmt.Errorf("failed to fetch revenue record: %w", err)
    } else {
        // Update the balance of the existing revenue record
        revenue.Balance = revenue.Balance.Add(fee) 
        if err := s.RevenueRepo.UpdateRevenue(&revenue); err != nil {
            return fmt.Errorf("failed to update revenue record: %w", err)
        }
    }

    return nil
}

