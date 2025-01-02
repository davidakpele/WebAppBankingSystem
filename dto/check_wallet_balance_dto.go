package dto

import "github.com/shopspring/decimal"

type AvailableBalance struct {
    ID       int64           `json:"id"`
    Balance  decimal.Decimal `json:"balance"`  
    Currency string          `json:"currency"`
}