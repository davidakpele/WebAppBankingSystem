package controllers

import (
	"encoding/json"
	"net/http"
	"strings"
	"wallet-app/security"
	"wallet-app/services"
	"github.com/gin-gonic/gin"
)
type CoinController struct {
    CoinService *services.CoinService
}

// NewCoinController initializes a new CoinController
func NewCoinController(coinService *services.CoinService) *CoinController {
    return &CoinController{CoinService: coinService}
}

func VerifyUser(c *gin.Context) {
    username := c.Param("username") 
    if username == "" {
		// If the username is missing, return an error
		c.JSON(http.StatusBadRequest, security.ErrorResponse{
			Code:   "BadRequest",
			Detail: "Username is required",
			Title:  "Missing Username",
			Status: http.StatusBadRequest,
		})
		return
	}
	// Verify user with the JWT token and get the user details
	userDetails, err := security.VerifyUserWithAuthService(c, username)
	if err != nil {
		// Return a structured error response if there's an error verifying the user
		c.JSON(http.StatusUnauthorized, security.ErrorResponse{
			Code:   "Unauthorized",
			Detail: err.Error(),
			Title:  "Invalid or missing token",
			Status: http.StatusUnauthorized,
		})
		return
	}

	// If no error, return the user details
	c.JSON(http.StatusOK, userDetails)
}

// SearchCoinHandler handles the search endpoint
func SearchCoinHandler(c *gin.Context) {
    // Get the keyword from the request parameters
    keyword := c.Query("q")
    if strings.TrimSpace(keyword) == "" {
        c.JSON(http.StatusBadRequest, gin.H{"error": "Keyword is required"})
        return
    }

    // Call the service to search the coin
    result, err := services.SearchCoinService(keyword)
    if err != nil {
        c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
        return
    }

    // Parse the JSON response from the service
    var jsonNode interface{}
    if err := json.Unmarshal([]byte(result), &jsonNode); err != nil {
        c.JSON(http.StatusInternalServerError, gin.H{"error": "Error parsing response"})
        return
    }

    // Return the parsed response as JSON
    c.JSON(http.StatusOK, jsonNode)
}

func GetTop50CoinsByMarketCapRank(c *gin.Context) {
    // Call the service to get the top 50 coins by market cap rank
    resp, err := services.GetTop50CoinsByMarketCapRank()
    if err != nil {
        c.JSON(http.StatusInternalServerError, gin.H{
            "error": "Failed to fetch data from CoinGecko API",
        })
        return
    }

    // Parse the response body into a JSON object and send it back as a response
    var jsonResponse interface{}
    if err := json.Unmarshal([]byte(resp), &jsonResponse); err != nil {
        c.JSON(http.StatusInternalServerError, gin.H{
            "error": "Error unmarshalling response",
        })
        return
    }

    c.JSON(http.StatusOK, jsonResponse)
}

func GetTrendingCoins(c *gin.Context) {
    // Call the service to get the trending coins
    resp, err := services.GetTrendingCoins()
    if err != nil {
        c.JSON(http.StatusInternalServerError, gin.H{
            "error": "Failed to fetch trending coins from CoinGecko API",
        })
        return
    }

    // Parse the response body into a JSON object and send it back as a response
    var jsonResponse interface{}
    if err := json.Unmarshal([]byte(resp), &jsonResponse); err != nil {
        c.JSON(http.StatusInternalServerError, gin.H{
            "error": "Error unmarshalling response",
        })
        return
    }

    c.JSON(http.StatusOK, jsonResponse)
}

// GetCoinDetails handles the GET request for coin details
func (cc *CoinController) GetCoinDetails(c *gin.Context) {
    coinID := c.Param("coinId")
    if coinID == "" {
        c.JSON(http.StatusBadRequest, gin.H{"error": "coinId is required"})
        return
    }

    // Get coin details from the service
    resp, err := cc.CoinService.GetCoinDetails(coinID)
    if err != nil {
        c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
        return
    }
    c.JSON(http.StatusOK, resp)
}

// GetAllCoins handles the request to fetch all coins from the database
func (cc *CoinController) GetAllCoins(c *gin.Context) {
	coins, err := cc.CoinService.GetAllCoins()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to fetch coins"})
		return
	}

	// Return the list of coins or an empty array if no records are found
	c.JSON(http.StatusOK, coins)
}

