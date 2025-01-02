package models

import (
	"time"
	"github.com/shopspring/decimal"
)

type EscrowStatus string

const (
	
	EscrowStatusPending        EscrowStatus = "PENDING"
	EscrowStatusFilled         EscrowStatus = "FILLED"
	EscrowStatusCancelled      EscrowStatus = "CANCELLED"
	EscrowStatusPartiallyFilled EscrowStatus = "PARTIALLY_FILLED"
	EscrowStatusError          EscrowStatus = "ERROR"
	EscrowStatusSuccess        EscrowStatus = "SUCCESS"
	EscrowStatusPendingPayment EscrowStatus = "PENDING_PAYMENT"
	EscrowStatusPaymentVerified EscrowStatus = "PAYMENT_VERIFIED"
	EscrowStatusOpen           EscrowStatus = "OPEN"
	EscrowStatusClosed         EscrowStatus = "CLOSED"
	EscrowStatusDisputed       EscrowStatus = "DISPUTED"
)

type Escrow struct {
	ID        uint            `gorm:"primaryKey;autoIncrement" json:"id"`
	OrderID   uint            `gorm:"not null" json:"order_id"`
	Amount    decimal.Decimal `gorm:"type:decimal(19,4);not null" json:"amount"`
	Status    EscrowStatus    `gorm:"type:enum('PENDING','FILLED','CANCELLED','PARTIALLY_FILLED','ERROR','SUCCESS','PENDING_PAYMENT','PAYMENT_VERIFIED','OPEN','CLOSED','DISPUTED')" json:"status"`
	CreatedAt time.Time       `gorm:"not null;autoCreateTime" json:"created_at"`
	UpdatedAt time.Time       `gorm:"not null;autoUpdateTime" json:"updated_at"`
}
