package payloads

import "math/big"

// BuySellRequest represents the request body for buying or selling crypto.
type BuySellRequest struct {
    Asset                 string    `json:"asset"`                 // The type of crypto being sold
    Amount                *big.Float `json:"amount"`               // The amount of crypto to sell
    RecipientWalletAddress string    `json:"recipient_wallet_address"` // Recipient's wallet address
}
