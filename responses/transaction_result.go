package responses

import "math/big"

// TransactionResult represents the result of a crypto transaction
type TransactionResult struct {
    TransactionID       string    `json:"transaction_id"`        // Unique ID of the transaction
    Status              string    `json:"status"`                // Status of the transaction (e.g., "success", "failed")
    Amount              *big.Float `json:"amount"`               // Amount of crypto sold or transferred
    RecipientWallet     string    `json:"recipient_wallet"`      // Wallet address of the recipient
    CryptoType          string    `json:"crypto_type"`           // Type of cryptocurrency (e.g., Bitcoin, Ethereum)
    TransactionDate     string    `json:"transaction_date"`      // Date of the transaction
    ConfirmationMessage string    `json:"confirmation_message"`  // Optional confirmation message
}
