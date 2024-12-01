package controllers

import (
	"net/http"
	"bytes"
	"encoding/json" 
	"github.com/gin-contrib/sessions"
	"github.com/gin-gonic/gin"
)

func Login(c *gin.Context) {
	var loginRequest struct {
		Username string `json:"username"`
		Password string `json:"password"`
	}

	// Bind the incoming JSON request
	if err := c.ShouldBindJSON(&loginRequest); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid request data"})
		return
	}

	// Prepare the request to Spring Boot's login endpoint
	url := "http://localhost:8095/auth/login" // Replace with your Spring Boot login URL
	jsonData, err := json.Marshal(loginRequest)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to marshal JSON"})
		return
	}

	// Create a new request
	req, err := http.NewRequest("POST", url, bytes.NewBuffer(jsonData))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to create request"})
		return
	}
	req.Header.Set("Content-Type", "application/json")

	// Send the request
	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to send request to Spring Boot"})
		return
	}
	defer resp.Body.Close()

	// Read the response body
	var responseData map[string]interface{}
	if err := json.NewDecoder(resp.Body).Decode(&responseData); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to decode response"})
		return
	}

	if resp.StatusCode != http.StatusOK {
		c.JSON(resp.StatusCode, gin.H{
			"error":   "Login failed",
			"message": responseData["message"], 
			"details": responseData["details"],  
		})
		return
	}

	// On successful login, store the user data in the session
	session := sessions.Default(c)

	session.Set("username", responseData["username"])
	session.Set("userId", responseData["userId"])
	
	// Save the session
	err = session.Save()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to save session"})
		return
	}

	// Return the successful login data
	c.JSON(http.StatusOK, gin.H{
		"message": "Login successful",
		"data":    responseData, 
	})
}

// Render the login page
func LoginPage(c *gin.Context) {
	tokenCookie, err := c.Cookie("token")
	if err == nil && tokenCookie != "" {
		// If token cookie exists, redirect to the protected route
		c.Redirect(http.StatusSeeOther, "/protected/home")
		return
	}
	// Render the login.html template
	c.HTML(http.StatusOK, "login.html", nil)
}

// Render the register page
func RegisterPage(c *gin.Context) {
	// Render the register.html template
	c.HTML(http.StatusOK, "register.html", nil)
}
