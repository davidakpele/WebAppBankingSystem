package routers

import (
    "wallet-app/controllers"
    "wallet-app/services"
    "wallet-app/middleware"
    "github.com/gin-gonic/gin"
)

// SetupRoutes initializes the routes for the application
func SetupRoutes(router *gin.Engine, base64Secret string, userService services.UserService, walletService services.WalletService) {
    // Instantiate controllers  
    walletController := controllers.NewWalletController(userService, walletService) 

    // Public Routes
    publicRoutes := router.Group("/")
    {
        publicRoutes.POST("/wallet/create", walletController.CreateWallets)
    }

    // Private Routes (requires JWT authentication)
    privateRoutes := router.Group("/")
    privateRoutes.Use(middleware.AuthenticationMiddleware(base64Secret))
    {
        privateRoutes.GET("/", controllers.Home)
        privateRoutes.GET("/users/:userId/balances", walletController.FetchAllBalances)
    }
}

