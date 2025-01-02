package payloads

import "github.com/shopspring/decimal"

type OrderRequest struct {
	UserID       uint            `json:"user_id" binding:"required"`
	TradingPair  string          `json:"trading_pair" binding:"required"`
	Type         string          `json:"type" binding:"required"`
	Price        decimal.Decimal `json:"price" binding:"required"`
	Amount       decimal.Decimal `json:"amount" binding:"required"`
	Currency     string          `json:"currency" binding:"required"`
	PaymentMethod     string     `json:"payment_method" binding:"required"`
	FilledAmount decimal.Decimal `json:"filled_amount" binding:"required"`
	Status       string          `json:"status" binding:"required"`
	BankID       uint            `json:"bank_id" binding:"required"`
	Remark       string          `json:"remark" binding:""`
	Signature    string          `json:"signature" binding:""`
}
