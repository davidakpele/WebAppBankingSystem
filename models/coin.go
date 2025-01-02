package models

import (
	"database/sql/driver"
	"fmt"
	"math/big"
	"time"

	"github.com/shopspring/decimal"
)

// BigInt wraps a big.Int for custom database serialization/deserialization
type BigInt struct {
	*big.Int
}

// Value implements the `driver.Valuer` interface for BigInt.
func (b BigInt) Value() (driver.Value, error) {
	if b.Int == nil {
		return nil, nil
	}
	return b.String(), nil
}

// Scan implements the `sql.Scanner` interface for BigInt.
func (b *BigInt) Scan(value interface{}) error {
	if value == nil {
		b.Int = nil
		return nil
	}

	switch v := value.(type) {
	case string:
		b.Int = new(big.Int)
		_, ok := b.Int.SetString(v, 10)
		if !ok {
			return fmt.Errorf("invalid value for BigInt: %s", v)
		}
	case []byte:
		b.Int = new(big.Int)
		_, ok := b.Int.SetString(string(v), 10)
		if !ok {
			return fmt.Errorf("invalid value for BigInt: %s", string(v))
		}
	default:
		return fmt.Errorf("unsupported type for BigInt: %T", value)
	}

	return nil
}

// Coin represents a cryptocurrency entity with associated metadata
type Coin struct {
	ID                           string          `gorm:"primary_key" json:"id"`
	Symbol                       string          `json:"symbol" validate:"required"`
	Name                         string          `json:"name"`
	Image                        string          `json:"image"`
	CurrentPrice                 decimal.Decimal `gorm:"type:decimal(19,4)" json:"current_price"`
	MarketCap                    BigInt          `json:"market_cap"`
	MarketCapRank                int             `json:"market_cap_rank"`
	FullyDilutedValuation        BigInt          `json:"fully_diluted_valuation"`
	TotalVolume                  BigInt          `json:"total_volume"`
	High24h                      BigInt          `json:"high_24h"`
	Low24h                       BigInt          `json:"low_24h"`
	PriceChange24h               decimal.Decimal `gorm:"type:decimal(19,4)" json:"price_change_24h"`
	PriceChangePercentage24h     decimal.Decimal `gorm:"type:decimal(19,4)" json:"price_change_percentage_24h"`
	MarketCapChange24h           BigInt          `json:"market_cap_change_24h"`
	MarketCapChangePercentage24h decimal.Decimal `gorm:"type:decimal(19,4)" json:"market_cap_change_percentage_24h"`
	CirculatingSupply            BigInt          `json:"circulating_supply"`
	TotalSupply                  BigInt          `json:"total_supply"`
	MaxSupply                    BigInt          `json:"max_supply"`
	Ath                          decimal.Decimal `gorm:"type:decimal(19,4)" json:"ath"`
	AthChangePercentage          decimal.Decimal `gorm:"type:decimal(19,4)" json:"ath_change_percentage"`
	AthDate                      *time.Time      `json:"ath_date"`
	Atl                          decimal.Decimal `gorm:"type:decimal(19,4)" json:"atl"`
	AtlChangePercentage          decimal.Decimal `gorm:"type:decimal(19,4)" json:"atl_change_percentage"`
	AtlDate                      *time.Time      `json:"atl_date"`
	Roi                          *ROI            `gorm:"type:json" json:"roi"`
	LastUpdated                  *time.Time      `json:"last_updated"`
}

// ROI represents the return on investment for a cryptocurrency
type ROI struct {
	Times      decimal.Decimal `json:"times"`
	Currency   string          `json:"currency"`
	Percentage decimal.Decimal `json:"percentage"`
}
