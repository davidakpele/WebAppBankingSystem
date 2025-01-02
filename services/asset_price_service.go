package services

import (
	"wallet-app/models"
	"wallet-app/repository"
	"github.com/shopspring/decimal"
)

type AssetPriceService struct {
	repo *repository.AssetPriceRepository
}

// NewAssetPriceService initializes the service
func NewAssetPriceService(repo *repository.AssetPriceRepository) *AssetPriceService {
	return &AssetPriceService{repo: repo}
}

// GetCurrentCryptoPrice fetches the current price for a given asset
func (s *AssetPriceService) GetCurrentCryptoPrice(asset string) (*decimal.Decimal, error) {
	assetPrice, err := s.repo.FindByAsset(asset)
	if err != nil {
		return nil, err
	}
	// Return the price as a decimal.Decimal
	price := decimal.NewFromFloat(assetPrice.Price)
	return &price, nil
}

// UpdateCryptoPrice updates the price of a given asset (for admin use)
func (s *AssetPriceService) UpdateCryptoPrice(asset string, price decimal.Decimal) error {
	assetPrice, err := s.repo.FindByAsset(asset)
	if err != nil {
		// If asset not found, create a new record
		assetPrice = &models.AssetPrice{Asset: asset, Price: price.InexactFloat64()}
	} else {
		// Update the existing price
		assetPrice.Price = price.InexactFloat64()
	}

	return s.repo.SavePrice(assetPrice)
}