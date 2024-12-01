package controllers

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strconv"
	"wallet-app/services"
	"github.com/gin-gonic/gin"
)

// CreateWalletsRequest represents the request body structure
type CreateWalletsRequest struct {
    UserID uint `json:"userId"`
}

// UserController handles user-related operations
type UserController struct {
    userService  services.UserService
    walletService services.WalletService 
}

// json error response for verify user
type SpringErrorResponse struct {
    Message    string `json:"message"`
    StatusCode int    `json:"statusCode"`
    Details    string `json:"details"`
    Target     string `json:"target"`
}

// json request for BuySell endpoint
type BuySellRequest struct {
    Action               string  `json:"action"`                // create or update
    OfferType            string  `json:"offerType"`             // SELL or BUY
    Asset                string  `json:"asset"`                 // Cryptocurrency ID like btc or eth
    Amount               float64 `json:"amount"`                // Amount of crypto
    Price                float64 `json:"price"`                 // Fiat currency (e.g. 3000)
    Currency             string  `json:"currency"`              // USD or NGN
    PaymentMethod        string  `json:"paymentMethod"`         // Payment method, e.g., bank-transfer or ewallet
    PaymentInstructions  string  `json:"paymentInstructions"`   // Payment instructions
    SellerId             string  `json:"sellerId"`              // Seller username
    BuyerId              string  `json:"buyerId"`               // Buyer username (optional)
    Signature            string  `json:"signature"`             // Transaction or trade signature
    RecipientWalletAddress string `json:"recipientWalletAddress"`// Recipient wallet address
}

// WalletController handles wallet-related requests
type WalletController struct {
    UserService  services.UserService
    WalletService services.WalletService 
}

// NewUserController creates a new UserController
func NewUserController(userService services.UserService, walletService services.WalletService) *UserController {
    return &UserController{
        userService:  userService,
        walletService: walletService,
    }
}

// NewWalletController creates a new WalletController
func NewWalletController(userService services.UserService, walletService services.WalletService) *WalletController {
    return &WalletController{
        UserService:   userService,
        WalletService: walletService,
    }
}

// VerifyUser checks if the user exists and validates the username via Spring Boot
func (uc *UserController) VerifyUser(c *gin.Context) {
    // Extract username from query parameters
    username := c.Query("username")

    // Send request to Spring Boot server with the username
    client := &http.Client{}
    req, err := http.NewRequest("GET", "http://localhost:8181/api/v2/collections/coin/verify-user?username="+username, nil)
    if err != nil {
        c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to create request"})
        return
    }

    // Set JWT token in Authorization header
    token := c.GetHeader("Authorization")
    req.Header.Set("Authorization", token)

    resp, err := client.Do(req)
    if err != nil {
        c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to send request to Spring Boot"})
        return
    }
    defer resp.Body.Close()

    body, err := io.ReadAll(resp.Body)
    if err != nil {
        c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to read response from Spring Boot"})
        return
    }

    if resp.StatusCode != http.StatusOK {
        // Parse the error response from Spring Boot
        var errorResponse SpringErrorResponse
        if err := json.Unmarshal(body, &errorResponse); err != nil {
            // If there's an error unmarshalling, just return the raw body
            c.JSON(resp.StatusCode, gin.H{"error": string(body)})
            return
        }

        // Return the parsed error response in JSON format
        c.JSON(resp.StatusCode, gin.H{
            "message":    errorResponse.Message,
            "statusCode": errorResponse.StatusCode,
            "details":    errorResponse.Details,
            "target":     errorResponse.Target,
        })
        return
    }

    // If successful, return the success response
    c.JSON(http.StatusOK, gin.H{"success": true})
}

func (wc *WalletController) FetchAllBalances(c *gin.Context) {
	// Extract user ID from request parameters or context (e.g., via JWT)
	userIdParam := c.Param("userId")
	userID, err := strconv.Atoi(userIdParam)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid user ID"})
		return
	}

	response, err := wc.WalletService.GetWalletBalance(uint(userID)) 
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, response)
}




func (wc *WalletController) CreateWallets(c *gin.Context) {
    if c.Request.Method != http.MethodPost {
        c.JSON(http.StatusMethodNotAllowed, gin.H{"error": "Invalid request method"})
        return
    }

    var req CreateWalletsRequest

    // Parse the incoming JSON request
    if err := c.ShouldBindJSON(&req); err != nil {
        c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid request body"})
        return
    }

    // Call the service to create wallets for the user
    err := wc.WalletService.CreateWalletsForUser(req.UserID)
    if err != nil {
        if err.Error() == fmt.Sprintf("user with ID %d does not exist", req.UserID) {
            c.JSON(http.StatusNotFound, gin.H{"error": "User does not exist"})
            return
        }
        if err.Error() == fmt.Sprintf("wallets for user ID %d already exist", req.UserID) {
            c.JSON(http.StatusConflict, gin.H{"error": "Wallets already exist for this user"})
            return
        }
        c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
        return
    }

    c.JSON(http.StatusOK, gin.H{"message": "Wallets created successfully for user", "userId": req.UserID})
}

