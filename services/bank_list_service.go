package services

import (
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"os"
	"wallet-app/dto"

	"github.com/joho/godotenv"
)

type BankListService struct {
	BaseURL string
}

// Initialize BankListService with a predefined base URL from the .env file
func NewBankListService() (*BankListService, error) {
    // Load environment variables from the .env file
    err := godotenv.Load()
    if err != nil {
        return nil, fmt.Errorf("Error loading .env file: %v", err)
    }

    // Get the base URL for the Deposit and Withdrawal service from environment variables
    baseURL := os.Getenv("DEPOSIT_AND_WITHDRA_SERVICE_URL")
    if baseURL == "" {
        return nil, fmt.Errorf("DEPOSIT_AND_WITHDRA_SERVICE_URL not set in .env file")
    }

    return &BankListService{
        BaseURL: baseURL,
    }, nil
}

// GetBankDetails fetches the bank details for a specific user and bank
func (s *BankListService) GetBankDetails(userID uint, bankID uint, bearerToken string) (*dto.BankListDTO, error) {
	// Construct the base URL for the BankList service
	baseURL := s.BaseURL
	if baseURL == "" {
		return nil, fmt.Errorf("BASE_URL not set in BankListService")
	}

	// Define the endpoint path and append userID and bankID dynamically
	parsedURL, err := url.JoinPath(baseURL, "api/v1/bank/user", fmt.Sprintf("%d", userID), "bank", fmt.Sprintf("%d", bankID))
	if err != nil {
		return nil, fmt.Errorf("failed to construct URL: %w", err)
	}

	// Create a new GET request with the constructed URL
	req, err := http.NewRequest("GET", parsedURL, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	// Add the Authorization header with the Bearer token
	req.Header.Set("Authorization", "Bearer "+bearerToken)

	// Send the GET request using the HTTP client
	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("request failed: %w", err)
	}
	defer resp.Body.Close()

	// Check if the status code is OK
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("failed to fetch bank details, status code: %d", resp.StatusCode)
	}

	// Decode the response body into BankListDTO
	var bankDetails dto.BankListDTO
	err = json.NewDecoder(resp.Body).Decode(&bankDetails)
	if err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	// Return the bank details
	return &bankDetails, nil
}



