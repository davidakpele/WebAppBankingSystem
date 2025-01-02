package payloads

import (
	"math/big"
	"github.com/shopspring/decimal"
)

// BuySellRequest represents the request body for buying or selling crypto.
type BuySellRequest struct {
    Action              string         `json:"action"`
    OfferType           string         `json:"offerType"`
    Asset               string         `json:"asset" binding:"required"`
    Amount              decimal.Decimal `json:"amount" binding:"required"`
    Price               decimal.Decimal `json:"price"`
    Currency            string         `json:"currency"` 
    PaymentMethod       string         `json:"paymentMethod"`
    PaymentInstructions string         `json:"paymentInstructions"`
    SellerId            string         `json:"sellerId" binding:"required"`
    BuyerId             string         `json:"buyerId"`
    Signature           string         `json:"signature"`
    RecipientWalletAddress string      `json:"recipientWalletAddress"`
}


type CoinWallet struct {
	ID       uint      `gorm:"primary_key"`
	UserId   uint      `gorm:"not null"`
	CryptoId string    `gorm:"not null"`
	Balance  *big.Float `gorm:"type:numeric(19,8)"`
}