package models

import (
	"time"

	"github.com/shopspring/decimal"
	"gorm.io/gorm"
)

type OrderType string

const (
	OrderTypeBuy  OrderType = "BUY"
	OrderTypeSell OrderType = "SELL"
)

type OrderStatus string

const (
	OrderStatusPending         OrderStatus = "PENDING"
	OrderStatusFilled          OrderStatus = "FILLED"
	OrderStatusPartiallyFilled OrderStatus = "PARTIALLY_FILLED"
	OrderStatusCancelled       OrderStatus = "CANCELLED"
	OrderStatusOpen            OrderStatus = "OPEN"
	OrderStatusClosed          OrderStatus = "CLOSED"
	OrderStatusPaymentVerified OrderStatus = "PAYMENT_VERIFIED"
)

type Order struct {
	ID           uint            `gorm:"primaryKey;autoIncrement" json:"id"`
	UserID       uint            `gorm:"not null" json:"user_id"`
	IsMaker      bool            `json:"is_maker"`
	TradingPair  string          `gorm:"not null" json:"trading_pair"` 
	Type         OrderType       `gorm:"type:enum('BUY','SELL')" json:"type"` 
	Price        decimal.Decimal `gorm:"type:decimal(19,2);not null" json:"price"` 
	Amount       decimal.Decimal `gorm:"type:decimal(19,2);not null" json:"amount"`  
	Status       OrderStatus     `gorm:"type:enum('PENDING', 'FILLED', 'PARTIALLY_FILLED', 'CANCELLED', 'OPEN', 'CLOSED', 'PAYMENT_VERIFIED')" json:"status"` 
	BankID       *uint           `gorm:"column:bank_id" json:"bank_id"` 
	ExpirationTime time.Time     `gorm:"not null;autoExpirationTime" json:"expiration_time"`
	CreatedAt    time.Time       `gorm:"not null;autoCreateTime" json:"created_at"`
	UpdatedAt    time.Time       `gorm:"not null;autoUpdateTime" json:"updated_at"`
}

// Hooks
func (o *Order) BeforeCreate(tx *gorm.DB) (err error) {
	now := time.Now()
	o.CreatedAt = now
	o.UpdatedAt = now
	return
}

func (o *Order) BeforeUpdate(tx *gorm.DB) (err error) {
	o.UpdatedAt = time.Now()
	return
}


func (order *Order) TableName() string {
	return "orders"
}