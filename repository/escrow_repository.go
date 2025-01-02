package repository

import (
	"errors"
	"gorm.io/gorm"
	"wallet-app/models"
)

type EscrowRepository interface {
	Save(escrow *models.Escrow) error
	FindByID(escrowID uint) (*models.Escrow, error)
	FindAllByStatus(status models.EscrowStatus) ([]models.Escrow, error)
	FindByOrderID(orderID uint) (*models.Escrow, error)
	Update(escrow *models.Escrow) error
	DeleteByOrderID(orderID uint) error
	DeleteByUserIDs(userIDs []uint) error
}

type escrowRepository struct {
	db *gorm.DB
}

// NewEscrowRepository creates a new EscrowRepository
func NewEscrowRepository(db *gorm.DB) EscrowRepository {
	return &escrowRepository{db: db}
}

// Save saves a new or existing escrow record
func (r *escrowRepository) Save(escrow *models.Escrow) error {
	return r.db.Save(escrow).Error
}

// FindByID retrieves an escrow by its ID
func (r *escrowRepository) FindByID(escrowID uint) (*models.Escrow, error) {
	var escrow models.Escrow
	err := r.db.First(&escrow, escrowID).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, nil // Escrow not found
	}
	return &escrow, err
}

// FindAllByStatus retrieves all escrows with the specified status
func (r *escrowRepository) FindAllByStatus(status models.EscrowStatus) ([]models.Escrow, error) {
	var escrows []models.Escrow
	err := r.db.Where("status = ?", status).Find(&escrows).Error
	if err != nil {
		return nil, err
	}
	return escrows, nil
}

// FindByOrderID retrieves an escrow by its associated order ID
func (r *escrowRepository) FindByOrderID(orderID uint) (*models.Escrow, error) {
	var escrow models.Escrow
	err := r.db.Where("order_id = ?", orderID).First(&escrow).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, nil // Escrow not found
	}
	return &escrow, err
}

// Update updates an existing escrow record
func (r *escrowRepository) Update(escrow *models.Escrow) error {
	return r.db.Save(escrow).Error 
}

// DeleteByOrderID deletes an escrow by its associated order ID
func (r *escrowRepository) DeleteByOrderID(orderID uint) error {
	err := r.db.Where("order_id = ?", orderID).Delete(&models.Escrow{}).Error
	return err
}

// DeleteByUserIDs deletes escrows for orders associated with specific user IDs
func (r *escrowRepository) DeleteByUserIDs(userIDs []uint) error {
	subQuery := r.db.Table("orders").Select("id").Where("user_id IN ?", userIDs)
	err := r.db.Where("order_id IN (?)", subQuery).Delete(&models.Escrow{}).Error
	return err
}
