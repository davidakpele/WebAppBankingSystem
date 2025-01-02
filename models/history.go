package models

import (
	"time"
	"github.com/shopspring/decimal"
)

type TransactionType string
type PaymentMethodType string


const (
	TransactionTypeBuy     TransactionType = "BUY"
	TransactionTypeSell    TransactionType = "SELL"
	TransactionTypeSend    TransactionType = "SEND"
	TransactionTypeReceive TransactionType = "RECEIVE"
)

const (
	BankTransferPayment PaymentMethodType = "BANK-TRANSFER"
	CreditOrDebitTransferPayment PaymentMethodType = "CREDIT-OR-DEBIT"
	CrytoCurrencyTransferPayment PaymentMethodType = "CRYPTO-CURRENCY"
	EWalletTransferPayment PaymentMethodType = "E-WALLET"
    MobileMoneyTransferPayment PaymentMethodType = "MOBILE-MONEY"
    CashDepositTransferPayment PaymentMethodType = "CASH-DEPOSIT"
    GiftCardTransferPayment PaymentMethodType = "GIFT-CARDd"
    WireTransferPayment PaymentMethodType = "WIRE-TRANSFER"
    QRCodeTransferPayment PaymentMethodType = "QR-CODE"
    PrepaidVouchersTransferPayment PaymentMethodType = "PREPAID-VOUCHERS"
    FiatP2PTransferPayment PaymentMethodType = "FIAT-P2P"
)

type History struct {
    ID        uint            `gorm:"primaryKey;autoIncrement" json:"id"`
    Type      TransactionType `gorm:"type:enum('BUY','SELL','SEND','RECEIVE');not null" json:"type"`
    Quantity  decimal.Decimal `gorm:"type:decimal(19,2)" json:"quantity"`
    Price     decimal.Decimal `gorm:"type:decimal(19,2)" json:"price"`   
    Fee       decimal.Decimal `gorm:"type:decimal(19,2)" json:"fee"`      
    CoinID    string          `gorm:"not null" json:"coin_id"`
    UserID    uint            `gorm:"not null" json:"user_id"`
    PaymentMethod PaymentMethodType  `gorm:"type:enum('BANK-TRANSFER', 'CREDIT-OR-DEBIT', 'CRYPTO-CURRENCY', 'E-WALLET', 'MOBILE-MONEY', 'CASH-DEPOSIT', 'GIFT-CARD', 'WIRE-TRANSFER', 'QR-CODE', 'PREPAID-VOUCHERS', 'FIAT-P2P')" json:"status"`
    CreatedOn time.Time       `gorm:"not null;autoCreateTime" json:"created_on"`
    UpdatedOn time.Time       `gorm:"not null;autoUpdateTime" json:"updated_on"`
}

