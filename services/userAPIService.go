package services

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"net/url"
	"os"
	"strconv"
	"wallet-app/dto"
	"github.com/go-resty/resty/v2"
	"github.com/joho/godotenv"
	"github.com/shopspring/decimal"
)

type UserAPIService interface {
	FindByUsername(username string) (*dto.UserDTO, error)
	FindByUserId(userId uint) (*dto.UserDTO, error)
    FetchBalanceByCurrency(userId int64, currencyType string, token string) (*dto.AvailableBalance, error)
    CreditSellerCurrencyWallet(fromUserId, creditorUserId uint, currencyWalletType string, amount decimal.Decimal, platformFee decimal.Decimal, tokenString string) error
}

type userAPIService struct {
    client *resty.Client
}

// Initialize service and load environment variables
func init() {
    if err := godotenv.Load(); err != nil {
        log.Fatalf("Error loading .env file: %v", err)
    }
}

func NewUserAPIService() UserAPIService {
	return &userAPIService{
		client: resty.New(),
	}
}

func (s *userAPIService) FindByUsername(username string) (*dto.UserDTO, error) {
    baseURL := os.Getenv("AUTHENTICATION_SERVICE_URL")
    if baseURL == "" {
        return nil, fmt.Errorf("AUTHENTICATION_SERVICE_URL not set in environment variables")
    }

    parsedURL, err := url.JoinPath(baseURL, "api/v1/user/by/public/username/", username)
    if err != nil {
        return nil, fmt.Errorf("failed to construct URL: %w", err)
    }

    resp, err := s.client.R().Get(parsedURL)
    if err != nil {
        return nil, fmt.Errorf("failed to send request: %w", err)
    }

    if resp.StatusCode() != 200 {
        return nil, fmt.Errorf("received non-200 response: %d, body: %s", resp.StatusCode(), resp.String())
    }

    var user dto.UserDTO
    if err := json.Unmarshal(resp.Body(), &user); err != nil {
        return nil, fmt.Errorf("failed to parse response: %w", err)
    }

    return &user, nil
}

func (us *userAPIService) FindByUserId(userId uint) (*dto.UserDTO, error) {
    baseURL := os.Getenv("AUTHENTICATION_SERVICE_URL")
    if baseURL == "" {
        return nil, fmt.Errorf("AUTHENTICATION_SERVICE_URL not set in environment variables")
    }

    userIdStr := strconv.Itoa(int(userId)) 
    parsedURL, err := url.JoinPath(baseURL, "api/v1/user/by/public/userId/", userIdStr)
    if err != nil {
        return nil, fmt.Errorf("failed to construct URL: %w", err)
    }

    resp, err := us.client.R().Get(parsedURL)
    if err != nil {
        return nil, fmt.Errorf("failed to send request: %w", err)
    }

    if resp.StatusCode() != 200 {
        return nil, fmt.Errorf("received non-200 response: %d, body: %s", resp.StatusCode(), resp.String())
    }

    var user dto.UserDTO
    if err := json.Unmarshal(resp.Body(), &user); err != nil {
        return nil, fmt.Errorf("failed to parse response: %w", err)
    }

    return &user, nil
}

// FetchBalanceByCurrency fetches the user's balance for the specified currency type
func (s *userAPIService) FetchBalanceByCurrency(userId int64, currencyType string, token string) (*dto.AvailableBalance, error) {
    baseURL := os.Getenv("DEPOSIT_AND_WITHDRA_SERVICE_URL")
    if baseURL == "" {
        return nil, fmt.Errorf("DEPOSIT_AND_WITHDRA_SERVICE_URL not set in environment variables")
    }

    reqURL := fmt.Sprintf("%s/api/v1/wallet/balance/userId/%d/currency/%s", baseURL, userId, url.PathEscape(currencyType))

    resp, err := s.client.R().SetHeader("Authorization", fmt.Sprintf("Bearer %s", token)).Get(reqURL)
    if err != nil {
        return nil, fmt.Errorf("failed to send request: %w", err)
    }

    if resp.StatusCode() != http.StatusOK {
        return nil, fmt.Errorf("received non-200 response: %d, body: %s", resp.StatusCode(), resp.String())
    }

    var balance dto.AvailableBalance
    if err := json.Unmarshal(resp.Body(), &balance); err != nil {
        return nil, fmt.Errorf("failed to parse response: %w", err)
    }

    return &balance, nil
}

// Implementation of CreditSellerCurrencyWallet method
func (s *userAPIService) CreditSellerCurrencyWallet(fromUserId, creditorUserId uint, currencyType string, amount decimal.Decimal, platformFee decimal.Decimal, tokenString string) error {
    baseURL := os.Getenv("DEPOSIT_AND_WITHDRA_SERVICE_URL")
    if baseURL == "" {
        return fmt.Errorf("DEPOSIT_AND_WITHDRA_SERVICE_URL not set in environment variables")
    }

    requestData := map[string]interface{}{
        "recipientUserId": fromUserId,
        "creditorUserId":  creditorUserId,
        "currencyType":    currencyType,
        "amount":          amount,
        "profit":          platformFee,
    }

    resp, err := s.client.R().
        SetHeader("Authorization", fmt.Sprintf("Bearer %s", tokenString)).
        SetHeader("Content-Type", "application/json").
        SetBody(requestData).
        Post(fmt.Sprintf("%s/api/v1/wallet/credit/account", baseURL))

    if err != nil {
        return fmt.Errorf("failed to send POST request: %w", err)
    }

    if resp.StatusCode() != http.StatusOK {
        return fmt.Errorf("received non-200 response: %d, body: %s", resp.StatusCode(), resp.String())
    }

    return nil
}

