package repository

import (
	"errors"

	"wallet-app/models"
	"gorm.io/gorm"
)

type AssetPriceRepository struct {
    db *gorm.DB
}

// NewAssetPriceRepository initializes the repository
func NewAssetPriceRepository(db *gorm.DB) *AssetPriceRepository {
    return &AssetPriceRepository{db: db}
}

// FindByAsset fetches the price of a specific asset
func (r *AssetPriceRepository) FindByAsset(asset string) (*models.AssetPrice, error) {
    var assetPrice models.AssetPrice
    if err := r.db.Where("asset = ?", asset).First(&assetPrice).Error; err != nil {
        if errors.Is(err, gorm.ErrRecordNotFound) {
            return nil, errors.New("asset not found")
        }
        return nil, err
    }
    return &assetPrice, nil
}

// SavePrice creates or updates an asset's price
func (r *AssetPriceRepository) SavePrice(assetPrice *models.AssetPrice) error {
    return r.db.Save(assetPrice).Error
}

// DeleteByID deletes an asset price record by its ID
func (r *AssetPriceRepository) DeleteByID(id uint) error {
    if err := r.db.Delete(&models.AssetPrice{}, id).Error; err != nil {
        return err
    }
    return nil
}
