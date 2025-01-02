package services

import (
	"database/sql/driver"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"math/big"
	"net/http"
	"time"
	"wallet-app/models"
    "wallet-app/responses"
	"wallet-app/repository"
	"github.com/shopspring/decimal"
)

type BigInt struct {
	*big.Int
}

// Value implements the `driver.Valuer` interface for BigInt.
func (b BigInt) Value() (driver.Value, error) {
	if b.Int == nil {
		return nil, nil
	}
	return b.String(), nil
}

// Scan implements the `sql.Scanner` interface for BigInt.
func (b *BigInt) Scan(value interface{}) error {
	if value == nil {
		b.Int = nil
		return nil
	}

	switch v := value.(type) {
	case string:
		b.Int = new(big.Int)
		_, ok := b.Int.SetString(v, 10)
		if !ok {
			return fmt.Errorf("invalid value for BigInt: %s", v)
		}
	case []byte:
		b.Int = new(big.Int)
		_, ok := b.Int.SetString(string(v), 10)
		if !ok {
			return fmt.Errorf("invalid value for BigInt: %s", string(v))
		}
	default:
		return fmt.Errorf("unsupported type for BigInt: %T", value)
	}

	return nil
}

type CoinService struct {
    CoinRepository    *repository.CoinRepository
    BaseURL           string
    UserService       UserAPIService  
    WalletService     WalletService
    AssetPriceService AssetPriceService
    WalletRepository  repository.WalletRepository
    HistoryService    *HistoryService
}


// Constructor for CoinService
func NewCoinService(coinRepository *repository.CoinRepository, 
    userService UserAPIService, 
    walletService WalletService, 
    assetPriceService AssetPriceService,
    walletRepository repository.WalletRepository,
    historyService *HistoryService) *CoinService {
    return &CoinService{
        CoinRepository:   coinRepository,
        BaseURL:          "https://api.coingecko.com/api/v3",
        UserService:      userService,
        WalletService:    walletService,
        AssetPriceService: assetPriceService,
        WalletRepository: walletRepository,
        HistoryService:   historyService,
    }
}

type CustomError struct {
    Response responses.SuccessResponse
}

func (e *CustomError) Error() string {
    return e.Response.Message
}

// WalletBalanceResponse represents the structure of the e-wallet response
type WalletBalanceResponse struct {
	CentralBalance string `json:"central_balance"`
}

type Coin struct {
    ID                           string          `gorm:"primary_key" json:"id"`
    Symbol                       string          `json:"symbol" validate:"required"`
    Name                         string          `json:"name"`
    Image                        map[string]string `json:"image"`
    CurrentPrice                 decimal.Decimal `gorm:"type:decimal(19,4)" json:"current_price"`
    MarketCap                    BigInt          `json:"market_cap"`
    MarketCapRank                int             `json:"market_cap_rank"`
    FullyDilutedValuation        BigInt          `json:"fully_diluted_valuation"`
    TotalVolume                  BigInt          `json:"total_volume"`
    High24h                      BigInt          `json:"high_24h"`
    Low24h                       BigInt          `json:"low_24h"`
    PriceChange24h               decimal.Decimal `gorm:"type:decimal(19,4)" json:"price_change_24h"`
    PriceChangePercentage24h     decimal.Decimal `gorm:"type:decimal(19,4)" json:"price_change_percentage_24h"`
    MarketCapChange24h           BigInt          `json:"market_cap_change_24h"`
    MarketCapChangePercentage24h decimal.Decimal `gorm:"type:decimal(19,4)" json:"market_cap_change_percentage_24h"`
    CirculatingSupply            BigInt          `json:"circulating_supply"`
    TotalSupply                  BigInt          `json:"total_supply"`
    MaxSupply                    BigInt          `json:"max_supply"`
    Ath                          decimal.Decimal `gorm:"type:decimal(19,4)" json:"ath"`
    AthChangePercentage          decimal.Decimal `gorm:"type:decimal(19,4)" json:"ath_change_percentage"`
    AthDate                      *time.Time      `json:"ath_date"`
    Atl                          decimal.Decimal `gorm:"type:decimal(19,4)" json:"atl"`
    AtlChangePercentage          decimal.Decimal `gorm:"type:decimal(19,4)" json:"atl_change_percentage"`
    AtlDate                      *time.Time      `json:"atl_date"`
    Roi                          *ROI            `gorm:"type:json" json:"roi"`
    LastUpdated                  *time.Time      `json:"last_updated"`
}

// ROI represents the return on investment for a cryptocurrency
type ROI struct {
    Times      decimal.Decimal `json:"times"`
    Currency   string          `json:"currency"`
    Percentage decimal.Decimal `json:"percentage"`
}

// SearchCoinService makes a request to the CoinGecko API and returns the response as a string
func SearchCoinService(keyword string) (string, error) {
	// Prepare the URL with the search query
	url := fmt.Sprintf("https://api.coingecko.com/api/v3/search?query=%s", keyword)

	// Make the HTTP GET request to CoinGecko API
	resp, err := http.Get(url)
	if err != nil {
		return "", fmt.Errorf("error making request: %v", err)
	}
	defer resp.Body.Close()

	// Check if the response status is OK (200)
	if resp.StatusCode != http.StatusOK {
		body, _ := ioutil.ReadAll(resp.Body)
		return "", fmt.Errorf("error response from CoinGecko: %s", body)
	}

	// Read the response body and return it
	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return "", fmt.Errorf("error reading response body: %v", err)
	}

	return string(body), nil
}

func GetTop50CoinsByMarketCapRank() (string, error) {
    url := fmt.Sprintf("https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&order=market_cap_desc&per_page=50&page=1&sparkline=false")

    // Create a new HTTP client and make the GET request
    resp, err := http.Get(url)
    if err != nil {
        return "", fmt.Errorf("error making request to CoinGecko API: %v", err)
    }
    defer resp.Body.Close()

    // Read the response body
    body, err := ioutil.ReadAll(resp.Body)
    if err != nil {
        return "", fmt.Errorf("error reading response from CoinGecko API: %v", err)
    }

    // Check for non-OK status codes
    if resp.StatusCode != http.StatusOK {
        return "", fmt.Errorf("CoinGecko API returned error: %s", body)
    }

    // Return the response body as a string
    return string(body), nil
}

func GetTrendingCoins() (string, error) {
    url := "https://api.coingecko.com/api/v3/search/trending"

    // Create a new HTTP client and make the GET request
    resp, err := http.Get(url)
    if err != nil {
        return "", fmt.Errorf("error making request to CoinGecko API: %v", err)
    }
    defer resp.Body.Close()

    // Read the response body
    body, err := ioutil.ReadAll(resp.Body)
    if err != nil {
        return "", fmt.Errorf("error reading response from CoinGecko API: %v", err)
    }

    // Check for non-OK status codes
    if resp.StatusCode != http.StatusOK {
        return "", fmt.Errorf("CoinGecko API returned error: %s", body)
    }

    // Return the response body as a string
    return string(body), nil
}

// GetCoinDetails fetches coin details from the API and processes the data
func (cs *CoinService) GetCoinDetails(coinID string) (Coin, error) {
    url := fmt.Sprintf("%s/coins/%s", cs.BaseURL, coinID)
    resp, err := http.Get(url)
    if err != nil {
        return Coin{}, errors.New("failed to fetch coin details")
    }
    defer resp.Body.Close()

    if resp.StatusCode != http.StatusOK {
        return Coin{}, fmt.Errorf("API responded with status code %d", resp.StatusCode)
    }

    body, err := ioutil.ReadAll(resp.Body)
    if err != nil {
        return Coin{}, errors.New("failed to read response body")
    }

    // Initialize the Coin struct
    var coin Coin
    if err := json.Unmarshal(body, &coin); err != nil {
        return Coin{}, fmt.Errorf("failed to parse response JSON: %w", err)
    }

    return coin, nil
}

// GetAllCoins fetches all coins from the repository
func (cs *CoinService) GetAllCoins() ([]models.Coin, error) {
	coins, err := cs.CoinRepository.FindAll()
	if err != nil {
		return nil, err
	}

	// Return the coins or an empty list
	return coins, nil
}



