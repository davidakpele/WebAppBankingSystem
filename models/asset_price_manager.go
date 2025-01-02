package models

import (
	"gorm.io/gorm"
	"time"
)

// AssetPrice represents the structure for cryptocurrency prices
type AssetPrice struct {
	gorm.Model
	ID        uint           `gorm:"primaryKey" json:"id"`
	Asset     string         `gorm:"unique;not null" json:"asset"`  
	Price     float64        `gorm:"not null" json:"price"`         
	UpdatedAt time.Time      `json:"updated_at"`                   
	CreatedAt time.Time      `json:"created_at"`
}
