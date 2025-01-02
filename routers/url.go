package routers

import (
    "wallet-app/controllers"
    "wallet-app/services"
    "wallet-app/middleware"
    "github.com/gin-gonic/gin"
)

// SetupRoutes initializes the routes for the application
func SetupRoutes(router *gin.Engine, base64Secret string, userService services.UserAPIService, walletService services.WalletService, coinService *services.CoinService, orderService services.OrderService) {
    // Instantiate controllers  
    walletController := controllers.NewWalletController(userService, walletService) 
    coinController := controllers.NewCoinController(coinService)
    orderController := controllers.NewOrderController(orderService, userService)
    homeController := controllers.NewDefaultController()
    // Public Routes
    publicRoutes := router.Group("/")
    {
        publicRoutes.POST("/coin/account/create", walletController.CreateWallets)
        publicRoutes.GET("/coin/username/:username", controllers.VerifyUser)
    }

    // Private Routes (requires JWT authentication)
    privateRoutes := router.Group("/")
    privateRoutes.Use(middleware.AuthenticationMiddleware(base64Secret))
    {
        privateRoutes.GET("/", homeController.Home)
        privateRoutes.GET("/coin/search", controllers.SearchCoinHandler)
        privateRoutes.GET("/coin/details/:coinId", coinController.GetCoinDetails)
        privateRoutes.GET("/coin/top50coins", controllers.GetTop50CoinsByMarketCapRank)
        privateRoutes.GET("/coin/get/trending", controllers.GetTrendingCoins)
        privateRoutes.GET("/users/:userId/balances", walletController.FetchAllBalances)
        privateRoutes.GET("/coin/all", coinController.GetAllCoins)
        privateRoutes.POST("/coin/wallet/sell/address", walletController.SellCryptoWithWalletAddress)
        privateRoutes.GET("/coin/user/wallet/key", walletController.GetUserWalletAddressKey)
        
        privateRoutes.POST("/order/create", orderController.CreateOrder)
        privateRoutes.GET("/order/:id", orderController.FetchByOrderId)
        privateRoutes.GET("/order/all", orderController.HandleFetchAllOrders)
        privateRoutes.POST("/orders/buy/p2p/assets", orderController.BuyAssetOnP2PMarket)
        privateRoutes.GET("/orders/details/:orderType", orderController.GetOrdersWithBankDetailsByOrderType)
        privateRoutes.PUT("/order/update/:id", orderController.UpdateOrder)
        privateRoutes.DELETE("/order/cancel/:id", orderController.CancelOrder)
        privateRoutes.DELETE("/order/delete/:id", orderController.DeleteOrderByOrderId)
        privateRoutes.GET("/orders/user/:userId", orderController.GetAllUserOrderList)
        privateRoutes.PUT("/orders/:orderId/status", orderController.UpdateOrderStatusForTradeCompletion)

    }
}

