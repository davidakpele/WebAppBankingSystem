package models

import (
	"time"
	"github.com/jinzhu/gorm"
)

type Order struct {
	gorm.Model
	ID            uint          `gorm:"primary_key" json:"id"`
	UserID        uint          `json:"user_id"`
	IsMaker       bool          `json:"is_maker"`
	TradingPair   string        `json:"trading_pair"`
	Type          OrderType     `gorm:"type:varchar(50)" json:"type"`
	Price         float64       `gorm:"type:decimal(19,4)" json:"price"`
	Amount        float64       `gorm:"type:decimal(19,4)" json:"amount"`
	FilledAmount  float64       `gorm:"type:decimal(19,4)" json:"filled_amount"`
	Status        OrderStatus   `gorm:"type:varchar(50)" json:"status"`
	BankID        uint          `json:"bank_id"`
	CreatedAt     time.Time     `json:"created_at"`
	UpdatedAt     time.Time     `json:"updated_at"`
}

type OrderType string

const (
	BuyOrder  OrderType = "BUY"
	SellOrder OrderType = "SELL"
)

type OrderStatus string

const (
	Open            OrderStatus = "OPEN"
	PartiallyFilled OrderStatus = "PARTIALLY_FILLED"
	Filled          OrderStatus = "FILLED"
	Canceled        OrderStatus = "CANCELED"
)
