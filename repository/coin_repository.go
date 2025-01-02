package repository

import (
	"wallet-app/models"

	"gorm.io/gorm"
)

type CoinRepository struct {
    DB *gorm.DB
}

func NewCoinRepository(db *gorm.DB) *CoinRepository {
    return &CoinRepository{DB: db}
}


// FindAll retrieves all coins from the database
func (cr *CoinRepository) FindAll() ([]models.Coin, error) {
	var coins []models.Coin
	result := cr.DB.Find(&coins)
	if result.Error != nil {
		return nil, result.Error
	}

	// Return an empty list if no coins are found
	if result.RowsAffected == 0 {
		return []models.Coin{}, nil
	}

	return coins, nil
}


// FindByTradingCoin retrieves a coin by its ID
func (cr *CoinRepository) FindByTradingCoin(coinId string) (*models.Coin, error) {
	var coin models.Coin
	result := cr.DB.Where("id = ?", coinId).First(&coin)
	if result.Error != nil {
		// Check if no rows were found
		if result.Error == gorm.ErrRecordNotFound {
			return nil, nil
		}
		return nil, result.Error
	}
	return &coin, nil
}