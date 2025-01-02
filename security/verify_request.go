package security

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v4"
	"github.com/joho/godotenv"
)

// ErrorResponse is a structured error response for easier readability
type ErrorResponse struct {
	Code   string `json:"code"`
	Detail string `json:"detail"`
	Title  string `json:"title"`
	Status int    `json:"status"`
}

// UserDTO is the user details struct
type UserDTO struct {
	ID        int      `json:"id"`
	Email     string   `json:"email"`
	Username  string   `json:"username"`
	CreatedOn string   `json:"createdOn"`
	UpdatedOn *string  `json:"updatedOn"`
	Enabled   bool     `json:"enabled"`
	Records   []Record `json:"records"`
}

// Record is a user record struct
type Record struct {
	ID           int    `json:"id"`
	FirstName    string `json:"firstName"`
	LastName     string `json:"lastName"`
	Gender       string `json:"gender"`
	Locked       bool   `json:"locked"`
	LockedAt     string `json:"lockedAt"`
	ReferralCode string `json:"referralCode"`
	ReferralLink string `json:"referralLink"`
	TransferPin  bool   `json:"_transfer_pin"`
	Blocked      bool   `json:"blocked"`
}

// VerifyUserWithAuthService verifies the user with the JWT token and retrieves the user details
func VerifyUserWithAuthService(c *gin.Context, username string) (*UserDTO, error) {
	// Load environment variables from .env file
	if err := godotenv.Load(); err != nil {
		return nil, fmt.Errorf("error loading .env file: %v", err)
	}

	// Retrieve Jwt secret key from environment variable
	jwtSecretKey := os.Getenv("JWT_SECRET_KEY")
	if jwtSecretKey == "" {
		return nil, fmt.Errorf("jwt secret key is not set in .env file")
	}

	// Get the Authorization header from the request
	authHeader := c.GetHeader("Authorization")
	if authHeader == "" {
		return nil, fmt.Errorf("authorization header is missing")
	}

	// Extract the token from the Authorization header (Bearer <token>)
	tokenString := strings.TrimPrefix(authHeader, "Bearer ")
	if tokenString == authHeader { 
		return nil, fmt.Errorf("authorization token format is incorrect")
	}

	// Decode the JWT token
	decodedKey, err := base64.StdEncoding.DecodeString(jwtSecretKey)
	if err != nil {
		return nil, fmt.Errorf("failed to decode the secret key: %v", err)
	}

	// Parse the JWT token
	token, err := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
		}
		return decodedKey, nil
	})

	if err != nil || !token.Valid {
		return nil, fmt.Errorf("invalid token: %v", err)
	}

	// Extract claims from the token
	if claims, ok := token.Claims.(jwt.MapClaims); ok && token.Valid {
		tokenUsername, ok := claims["sub"].(string)
		if !ok || tokenUsername == "" {
			return nil, fmt.Errorf("username not found in token claims or empty")
		}
		
		// Retrieve user details from the Authentication Service
		userDetails, err := getUserDetailsFromSpringBoot(tokenUsername)
		if err != nil {
			return nil, fmt.Errorf("failed to retrieve user details from Authentication Service: %v", err)
		}

		// Normalize and compare the username from the token with the username in the response
		if strings.TrimSpace(strings.ToLower(username)) != strings.TrimSpace(strings.ToLower(userDetails.Username)) {
			return nil, fmt.Errorf("username from token does not match the username in the response")
		}
		fmt.Printf("Comparing token username: '%s' with user details username: '%s'\n", tokenUsername, userDetails.Username)

		// Return user details if the username matches
		return userDetails, nil
	}

	return nil, fmt.Errorf("invalid token claims")
}

// getUserDetailsFromSpringBoot makes a request to the Spring Boot service and retrieves user details
func getUserDetailsFromSpringBoot(username string) (*UserDTO, error) {
	// Authentication Service endpoint to find user by username
	springBootURL := fmt.Sprintf("http://localhost:8080/api/v1/user/by/public/username/%s", username)
	
	// Make the HTTP GET request to Authentication Service
	resp, err := http.Get(springBootURL)
	if err != nil {
		return nil, fmt.Errorf("error making request to Authentication Service: %v", err)
	}
	defer resp.Body.Close()

	// Read the response body
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("error reading response from Authentication Service: %v", err)
	}

	// Check if the response status is OK (200)
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("authentication service returned error: %s", body)
	}

	// Parse the JSON response into a UserDTO struct
	var user UserDTO
	err = json.Unmarshal(body, &user)
	if err != nil {
		return nil, fmt.Errorf("error unmarshalling response into UserDTO: %v", err)
	}

	// Return the user details
	return &user, nil
}


// Extract jwt header token and return it 
func ExtractHeaderToken(c *gin.Context) (string, error) {
    authHeader := c.GetHeader("Authorization")
     if authHeader == "" {
		return "", fmt.Errorf("authorization header is missing")
    }

    // 2 Split the header value into "Bearer" and the token
    parts := strings.Split(authHeader, " ")
    if len(parts) != 2 || strings.ToLower(parts[0]) != "bearer" {
		return "", fmt.Errorf("invalid authorization header format")
    }

	token := parts[1]
    return token, nil
}

