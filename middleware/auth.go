package middleware

import (
    "encoding/base64"
    "fmt"
    "net/http"
    "strings"
    "github.com/golang-jwt/jwt/v4"
    "github.com/gin-gonic/gin"
)

type ErrorResponse struct {
    Code   string `json:"code"`
    Detail string `json:"detail"`
    Title  string `json:"title"`
    Status int    `json:"status"`
}

func AuthenticationMiddleware(base64Secret string) gin.HandlerFunc {
    return func(c *gin.Context) {
        // Get the JWT from the Authorization header
        authHeader := c.GetHeader("Authorization")
        if authHeader == "" {
            errorResponse := ErrorResponse{
                Code:   "missing_authorization_header",
                Detail: "Missing Authorization header",
                Title:  "Authentication Error",
                Status: http.StatusUnauthorized,
            }
            c.JSON(http.StatusUnauthorized, gin.H{"error": errorResponse})
            c.Abort()
            return
        }

        // Decode the base64 secret key
        decodedKey, err := base64.StdEncoding.DecodeString(base64Secret)
        if err != nil {
            errorResponse := ErrorResponse{
                Code:   "invalid_secret_key",
                Detail: "Failed to decode secret key",
                Title:  "Authentication Error",
                Status: http.StatusUnauthorized,
            }
            c.JSON(http.StatusUnauthorized, gin.H{"error": errorResponse})
            c.Abort()
            return
        }

        // Extract token from the Authorization header
        tokenString := strings.Split(authHeader, " ")[1]
        token, err := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
            if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
                return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
            }
            return decodedKey, nil
        })

        if err != nil {
            errorResponse := ErrorResponse{
                Code:   "invalid_token",
                Detail: fmt.Sprintf("Token error: %v", err),
                Title:  "Authentication Error",
                Status: http.StatusUnauthorized,
            }
            c.JSON(http.StatusUnauthorized, gin.H{"error": errorResponse})
            c.Abort()
            return
        }

        if !token.Valid {
            errorResponse := ErrorResponse{
                Code:   "invalid_token",
                Detail: "Invalid token",
                Title:  "Authentication Error",
                Status: http.StatusUnauthorized,
            }
            c.JSON(http.StatusUnauthorized, gin.H{"error": errorResponse})
            c.Abort()
            return
        }

        // Extract claims if the token is valid
        if claims, ok := token.Claims.(jwt.MapClaims); ok && token.Valid {
            c.Set("userID", claims["sub"])
            c.Set("roles", claims["role"])
        } else {
            errorResponse := ErrorResponse{
                Code:   "invalid_token_claims",
                Detail: "Invalid token claims",
                Title:  "Authentication Error",
                Status: http.StatusUnauthorized,
            }
            c.JSON(http.StatusUnauthorized, gin.H{"error": errorResponse})
            c.Abort()
            return
        }

        // Proceed to the next middleware/handler
        c.Next()
    }
}
