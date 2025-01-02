package controllers

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"strconv"
	"strings"
	"wallet-app/payloads"
	"wallet-app/services"
	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v4"
	"github.com/joho/godotenv"
	"github.com/shopspring/decimal"
)

// CreateWalletsRequest represents the request body structure
type CreateWalletsRequest struct {
    UserID uint `json:"userId"`
}

// UserController handles user-related operations
type UserController struct {
    userService  services.UserAPIService
    walletService services.WalletService 
}

// json error response for verify user
type SpringErrorResponse struct {
    Message    string `json:"message"`
    StatusCode int    `json:"statusCode"`
    Details    string `json:"details"`
    Target     string `json:"target"`
}

// WalletController handles wallet-related requests
type WalletController struct {
    UserService  services.UserAPIService
    WalletService services.WalletService 
}

// NewUserController creates a new UserController
func NewUserController(userService services.UserAPIService, walletService services.WalletService) *UserController {
    return &UserController{
        userService:  userService,
        walletService: walletService,
    }
}

// NewWalletController creates a new WalletController
func NewWalletController(userService services.UserAPIService, walletService services.WalletService) *WalletController {
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

func (ctrl *WalletController) SellCryptoWithWalletAddress(c *gin.Context) {
    var request payloads.BuySellRequest
    if err := c.ShouldBindJSON(&request); err != nil {
        // Log the error for debugging
        c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid request body", "details": err.Error()})
        return
    }

    if strings.TrimSpace(request.RecipientWalletAddress) == "" {
        c.JSON(http.StatusBadRequest, gin.H{"error": "Please provide recipient wallet address"})
        return
    }

    // Check if Amount is zero or less than zero
    if request.Amount.IsZero() || request.Amount.Cmp(decimal.NewFromFloat(0)) <= 0 {
        c.JSON(http.StatusBadRequest, gin.H{"error": "Please provide a valid amount greater than zero"})
        return
    }

    // Use c.Request to access the HTTP request
    result, err := ctrl.WalletService.SellWithAddress(&request, c.Request)
    if err != nil {
        c.JSON(http.StatusBadRequest, gin.H{"error": fmt.Sprintf("Details: %v", err)})
        return
    }

    c.JSON(http.StatusOK, result)
}

func (ctrl *WalletController) GetUserWalletAddressKey(c *gin.Context) {
    // Load environment variables
    if err := godotenv.Load(); err != nil {
        c.JSON(http.StatusForbidden, gin.H{"error": "Error loading .env file"})
        return
    }

    // Get the JWT secret key from environment variables
    jwtSecretKey := os.Getenv("JWT_SECRET_KEY")
    if jwtSecretKey == "" {
        c.JSON(http.StatusInternalServerError, gin.H{"error": "JWT secret key is not set in .env file"})
        return
    }

    // Get the Authorization header
    authHeader := c.GetHeader("Authorization")
    if authHeader == "" {
        c.JSON(http.StatusForbidden, gin.H{"error": "Authorization header is missing"})
        return
    }

    // Extract the token
    tokenString := strings.TrimPrefix(authHeader, "Bearer ")
    if tokenString == authHeader {
        c.JSON(http.StatusForbidden, gin.H{"error": "Authorization token format is incorrect"})
        return
    }

    // Decode the JWT secret key
    decodedKey, err := base64.StdEncoding.DecodeString(jwtSecretKey)
    if err != nil {
        c.JSON(http.StatusForbidden, gin.H{"error": "Failed to decode the secret key"})
        return
    }

    // Parse the token
    token, _:= jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
        if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
            return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
        }
        return decodedKey, nil
    })

    if claims, ok := token.Claims.(jwt.MapClaims); ok && token.Valid {
        // Extract the username from the token claims
        tokenUsername, ok := claims["sub"].(string)
        if !ok || tokenUsername == "" {
            c.JSON(http.StatusForbidden, gin.H{"error": "Username not found in token claims or empty"})
            return
        }

        // Get the service type from the query parameters
        serviceType := c.Query("serviceType")
        if strings.TrimSpace(serviceType) == "" {
            c.JSON(http.StatusBadRequest, gin.H{"error": "Please provide wallet service, e.g., bitcoin or solana"})
            return
        }

        // Fetch user details
        userDTO, err := ctrl.UserService.FindByUsername(tokenUsername)
        if err != nil || userDTO.Username != tokenUsername {
            c.JSON(http.StatusForbidden, gin.H{"error": "Unauthorized access"})
            return
        }

        // Fetch the wallet address key
        addressKey, err := ctrl.WalletService.GetUserWalletAddressKey(serviceType, uint(userDTO.ID))
        if err != nil {
            c.JSON(http.StatusBadRequest, gin.H{"error": fmt.Sprintf("Error : %v", err)})
            return
        }

        // Respond with the address key
        c.JSON(http.StatusOK, gin.H{"addressKey": addressKey})
    } else {
        c.JSON(http.StatusForbidden, gin.H{"error": "Invalid token"})
    }
}