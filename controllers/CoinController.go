package controllers

import (
	"github.com/gin-gonic/gin"
	"net/http"
	"github.com/gin-contrib/sessions"
)

// HomeController - Handles home related routes
func Home(c *gin.Context) {
    session := sessions.Default(c) // Access session
    username := session.Get("username") // Retrieve username from session
    if username == nil {
        c.JSON(http.StatusUnauthorized, gin.H{"error": "Not logged in"})
        return
    }
    c.JSON(http.StatusOK, gin.H{
        "message":  "You are logged in",
        "username": username,
    })
}

