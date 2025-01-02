package models

import "time"

type PaymentSettings struct {
	ID       uint  `gorm:"primaryKey"`
	OrderID  uint  `gorm:"not null"`
	SellerID uint  `gorm:"not null"`
	Remark   string          `gorm:"not null" json:"remark"`
	Signature string          `gorm:"not null" json:"signature"`
	PaymentMethod PaymentMethodType  `gorm:"type:enum('BANK-TRANSFER', 'CREDIT-OR-DEBIT', 'CRYPTO-CURRENCY', 'E-WALLET', 'MOBILE-MONEY', 'CASH-DEPOSIT', 'GIFT-CARD', 'WIRE-TRANSFER', 'QR-CODE', 'PREPAID-VOUCHERS', 'FIAT-P2P')" json:"status"`
	CreatedOn time.Time       `gorm:"not null;autoCreateTime" json:"created_on"`
    UpdatedOn time.Time       `gorm:"not null;autoUpdateTime" json:"updated_on"`
}

// TableName overrides the default table name
func (PaymentSettings) TableName() string {
	return "order_payment_instructions"
}
