package repository

import (
    "wallet-app/models"
    "gorm.io/gorm"
)

// RevenueRepository is the repository for handling revenue-related database operations
type RevenueRepository struct {
    DB *gorm.DB
}

// NewRevenueRepository creates a new instance of RevenueRepository
func NewRevenueRepository(db *gorm.DB) *RevenueRepository {
    return &RevenueRepository{
        DB: db,
    }
}

// GetRevenueByID fetches revenue by ID from the database
func (r *RevenueRepository) GetRevenueByID(id uint) (*models.Revenue, error) {
    var revenue models.Revenue
    if err := r.DB.First(&revenue, id).Error; err != nil {
        return nil, err
    }
    return &revenue, nil
}

// AddRevenue adds a new revenue record to the database
func (r *RevenueRepository) AddRevenue(revenue *models.Revenue) error {
    if err := r.DB.Create(revenue).Error; err != nil {
        return err
    }
    return nil
}

// UpdateRevenue updates the revenue balance in the database
func (r *RevenueRepository) UpdateRevenue(revenue *models.Revenue) error {
    if err := r.DB.Save(revenue).Error; err != nil {
        return err
    }
    return nil
}


