package dto

type BankListDTO struct {
	ID                uint   `json:"id"`
	BankCode          string `json:"bankCode"`
	BankName          string `json:"bankName"`
	AccountHolderName string `json:"accountHolderName"`
	AccountNumber     string `json:"accountNumber"`
}
