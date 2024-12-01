package models

import (
	"time"
)

type Coin struct {
	ID                        string    `gorm:"primary_key" json:"id"`
	Symbol                    string    `json:"symbol"`
	Name                      string    `json:"name"`
	Image                     string    `json:"image"`
	CurrentPrice              float64   `gorm:"type:decimal(19,4)" json:"current_price"`
	MarketCap                 string    `json:"market_cap"` // Use string to handle big.Int
	MarketCapRank             int       `json:"market_cap_rank"`
	FullyDilutedValuation     string    `json:"fully_diluted_valuation"` // Use string to handle big.Int
	TotalVolume               string    `json:"total_volume"` // Use string to handle big.Int
	High24h                   string    `json:"high_24h"`     // Use string to handle big.Int
	Low24h                    string    `json:"low_24h"`      // Use string to handle big.Int
	PriceChange24h            float64   `gorm:"type:decimal(19,4)" json:"price_change_24h"`
	PriceChangePercentage24h  float64   `gorm:"type:decimal(19,4)" json:"price_change_percentage_24h"`
	MarketCapChange24h        string    `json:"market_cap_change_24h"` // Use string to handle big.Int
	MarketCapChangePercentage24h float64 `gorm:"type:decimal(19,4)" json:"market_cap_change_percentage_24h"`
	CirculatingSupply         string    `json:"circulating_supply"` // Use string to handle big.Int
	TotalSupply               string    `json:"total_supply"` // Use string to handle big.Int
	MaxSupply                 string    `json:"max_supply"` // Use string to handle big.Int
	Ath                       float64   `gorm:"type:decimal(19,4)" json:"ath"`
	AthChangePercentage       float64   `gorm:"type:decimal(19,4)" json:"ath_change_percentage"`
	AthDate                   *time.Time `json:"ath_date"`
	Atl                       float64   `gorm:"type:decimal(19,4)" json:"atl"`
	AtlChangePercentage       float64   `gorm:"type:decimal(19,4)" json:"atl_change_percentage"`
	AtlDate                   *time.Time `json:"atl_date"`
	Roi                       *ROI      `gorm:"type:json" json:"roi"` // JSON serialization for nested struct
	LastUpdated               *time.Time `json:"last_updated"`
}

type ROI struct {
	Times      float64 `json:"times"`
	Currency   string  `json:"currency"`
	Percentage float64 `json:"percentage"`
}

// MarshalJSON and UnmarshalJSON can be added for `ROI` if further customization is needed.
