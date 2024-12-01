package models

import (
	"time"
	"github.com/jinzhu/gorm"
)

type CoinRevenue struct {
	gorm.Model
	ID        uint      `gorm:"primary_key" json:"id"`
	Balance   float64   `gorm:"type:decimal(19,4)" json:"balance"`
	CreatedAt time.Time `json:"created_at"`
}
