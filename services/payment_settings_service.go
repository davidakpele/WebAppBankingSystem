package services

import (
	// "wallet-app/models"
	"errors"
	"time"
	"wallet-app/models"
	"wallet-app/repository"
)

type PaymentSettingService struct {
	repo repository.PaymentSettingRepository
}

type PaymentSettingServiceInterface interface {
	Create(orderId uint, sellerId uint, remark string, signature string, paymentMethod *models.PaymentMethodType) error
	Update(Id uint, updatePaymentSettings models.PaymentSettings) error
	DeleteByOrderIdAndUserId(orderID uint, userID uint) error
}

// NewPaymentSettingService initializes the service
func NewPaymentSettingService(repo repository.PaymentSettingRepository) *PaymentSettingService {
	return &PaymentSettingService{repo: repo}
}

// Implementing the methods
func (s *PaymentSettingService) Create(orderId uint, sellerId uint, remark string, signature string, paymentMethod models.PaymentMethodType) error {
	// Validate the input
	if orderId == 0 || sellerId == 0 || remark == "" || signature == "" {
		return errors.New("invalid input: all fields are required")
	}

	// Create a new PaymentSettings instance
	paymentSettings := &models.PaymentSettings{
		OrderID:       orderId,
		SellerID:      sellerId,
		Remark:        remark,
		Signature:     signature,
		PaymentMethod: paymentMethod,
		CreatedOn:     time.Now(),
		UpdatedOn:     time.Now(),
	}

	// Save to the database using the repository
	if err := s.repo.Save(paymentSettings); err != nil {
		return err
	}

	return nil
}

// Update updates an existing payment setting
func (s *PaymentSettingService) Update(id uint, updatePaymentSettings models.PaymentSettings) error {
	// Validate the input
	if id == 0 {
		return errors.New("invalid ID: ID cannot be zero")
	}

	// Fetch the existing payment settings by ID
	existingPaymentSettings, err := s.repo.FindByUserIdAndOrderId(updatePaymentSettings.OrderID, updatePaymentSettings.SellerID)
	if err != nil {
		return err
	}

	// Update the fields
	existingPaymentSettings.Remark = updatePaymentSettings.Remark
	existingPaymentSettings.Signature = updatePaymentSettings.Signature
	existingPaymentSettings.PaymentMethod = updatePaymentSettings.PaymentMethod
	existingPaymentSettings.UpdatedOn = time.Now()

	// Save the updated payment settings
	if err := s.repo.Update(existingPaymentSettings); err != nil {
		return err
	}

	return nil
}

// DeleteByOrderIdAndUserId deletes payment settings by order ID and user ID
func (s *PaymentSettingService) DeleteByOrderIdAndUserId(orderID uint, userID uint) error {
	// Validate the input
	if orderID == 0 || userID == 0 {
		return errors.New("invalid input: orderID and userID cannot be zero")
	}

	// Delete the payment settings using the repository
	if err := s.repo.DeleteByOrderIdAndUserId(orderID, userID); err != nil {
		return err
	}

	return nil
}
