package repository

import (
	"errors"
	"wallet-app/models"
	"gorm.io/gorm"
)

type PaymentSettingRepository interface {
	Save(paymentSettings *models.PaymentSettings) error
	FindByUserIdAndOrderId(orderId uint, userId uint) (*models.PaymentSettings, error)
	Update(paymentSettings *models.PaymentSettings) error
	DeleteByOrderIdAndUserId(orderId uint, userId uint) error
}

type paymentSettingRepository struct {
	db *gorm.DB
}

func NewPaymentSettingRepository(db *gorm.DB) PaymentSettingRepository {
	return &paymentSettingRepository{db: db}
}

// Save saves a new PaymentSettings record in the database
func (p *paymentSettingRepository) Save(paymentSettings *models.PaymentSettings) error {
	if paymentSettings == nil {
		return errors.New("paymentSettings cannot be nil")
	}
	result := p.db.Create(paymentSettings)
	return result.Error
}

// FindByID fetches a PaymentSettings record by orderId and userId
func (p *paymentSettingRepository) FindByUserIdAndOrderId(orderId uint, userId uint) (*models.PaymentSettings, error) {
	var paymentSettings models.PaymentSettings
	result := p.db.Where("order_id = ? AND seller_id = ?", orderId, userId).First(&paymentSettings)
	if result.Error != nil {
		if errors.Is(result.Error, gorm.ErrRecordNotFound) {
			return nil, nil
		}
		return nil, result.Error
	}
	return &paymentSettings, nil
}

// Update updates an existing PaymentSettings record
func (p *paymentSettingRepository) Update(paymentSettings *models.PaymentSettings) error {
	if paymentSettings == nil {
		return errors.New("paymentSettings cannot be nil")
	}
	result := p.db.Save(paymentSettings)
	return result.Error
}

// DeleteByOrderIdAndUserId deletes a PaymentSettings record by orderId and userId
func (p *paymentSettingRepository) DeleteByOrderIdAndUserId(orderId uint, userId uint) error {
	result := p.db.Where("order_id = ? AND seller_id = ?", orderId, userId).Delete(&models.PaymentSettings{})
	return result.Error
}


