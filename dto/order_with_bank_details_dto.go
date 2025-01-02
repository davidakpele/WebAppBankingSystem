package dto

import "wallet-app/models"

type OrderWithBankDetailsDTO struct {
	Order      models.Order       `json:"order"`
	BankDetails BankListDTO `json:"bank_details"`
}