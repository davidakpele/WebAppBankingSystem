package models

import (
	"time"
	"github.com/jinzhu/gorm"
)

type CoinTransactionHistory struct {
	gorm.Model
	ID        uint            `gorm:"primary_key" json:"id"`
	Type      TransactionType `gorm:"type:varchar(50)" json:"type"`
	Quantity  float64         `gorm:"type:decimal(19,4)" json:"quantity"`
	Price     float64         `gorm:"type:decimal(19,4)" json:"price"`
	Fee       float64         `gorm:"type:decimal(19,4)" json:"fee"`
	CoinID    string          `json:"coin_id"`
	UserID    uint            `json:"user_id"`
	CreatedOn time.Time       `json:"created_on"`
	UpdatedOn time.Time       `json:"updated_on"`
}

type TransactionType string

const (
	Buy    TransactionType = "BUY"
	Sell   TransactionType = "SELL"
	Send   TransactionType = "SEND"
	Receive TransactionType = "RECEIVE"
)
