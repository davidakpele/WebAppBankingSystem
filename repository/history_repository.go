package repository

import (
	"log"
	"wallet-app/models"

	"gorm.io/gorm"
)


type HistoryRepository interface {
	SaveHistory(history *models.History) error 
	FindHistoryByUserId(userID uint) ([]models.History, error) 
}

type historyRepository struct {
	db *gorm.DB
}

// Constructor for HistoryRepository
func NewHistoryRepository(db *gorm.DB) HistoryRepository {
	return &historyRepository{db: db}
}

// SaveHistory saves a new transaction history record
func (r *historyRepository) SaveHistory(history *models.History) error {
	// Attempt to create the record in the database
	err := r.db.Create(history).Error

	// If an error occurs, log it and return it
	if err != nil {
		// Log the error to identify the issue
		log.Printf("Error saving history: %v", err)
		return err
	}

	// If no error, return nil
	return nil
}

// FindHistoryByUserId fetches transaction histories by User ID
func (r *historyRepository) FindHistoryByUserId(userID uint) ([]models.History, error) {
	var histories []models.History
	err := r.db.Where("user_id = ?", userID).Order("created_on DESC").Find(&histories).Error
	if err != nil {
		return nil, err
	}
	return histories, nil
}
