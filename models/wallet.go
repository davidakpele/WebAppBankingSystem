package models

import (
    "time"
     "gorm.io/gorm"
)


type Wallet struct {
    gorm.Model
    ID           uint    `gorm:"primaryKey"`
    Balance       float64 `json:"balance"`
    CryptoID      string  `json:"crypto_id"`  
    UserID        uint    `json:"user_id"`  
    Version      int     `gorm:"default:1"`
    WalletAddress string  `json:"wallet_address"`
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