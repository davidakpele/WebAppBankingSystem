package models

import (
	"time"
	"gorm.io/gorm"
	"github.com/shopspring/decimal"
)


type Wallet struct {
    gorm.Model
    ID           uint            `gorm:"primaryKey;autoIncrement" json:"id"`
    UserID       uint            `gorm:"not null" json:"user_id"`  
    CryptoID     string          `gorm:"not null" json:"crypto_id"` 
    Balance      decimal.Decimal `gorm:"type:decimal(19,2)" json:"balance"` 
    FillAmount    decimal.Decimal `gorm:"type:decimal(19,2)" json:"fill_amount"`
    WalletAddress string         `json:"wallet_address"`     
    Version      int             `gorm:"version" json:"-"` 
}

// User represents the users table in the database
type User struct {
    ID        uint      `gorm:"primaryKey"`
    Username  string    `gorm:"unique;not null"`
    Email     string    `gorm:"unique;not null"`
    Password  string    `gorm:"not null"`
    CreatedAt time.Time
    UpdatedAt time.Time
}