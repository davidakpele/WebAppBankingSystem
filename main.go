package main

import (
	"log"
	"os"
	"wallet-app/db"
	"wallet-app/repository"
	"wallet-app/services"
	"wallet-app/routers"
	"github.com/gin-gonic/gin"
	"github.com/gin-contrib/cors"
	"github.com/joho/godotenv"
	"html/template"
	"github.com/gin-contrib/sessions"
	"github.com/gin-contrib/sessions/cookie"
)

func main() {
	// Load environment variables from .env file
	if err := godotenv.Load(); err != nil {
		log.Fatal("Error loading .env file")
	}

	// Connect to the database
	db.Connect()

	// Retrieve the JWT secret key from the environment variable
	jwtSecretKey := os.Getenv("JWT_SECRET_KEY")
	if jwtSecretKey == "" {
		log.Fatal("JWT_SECRET_KEY is not set in .env file")
	}

	// Create router
	router := gin.Default()

	// CORS configuration
	router.Use(cors.Default())
	router.Static("/static", "./static")

	// Set up session store using the JWT secret key
	store := cookie.NewStore([]byte(jwtSecretKey))
	router.Use(sessions.Sessions("session_name", store))

	// Set up HTML template engine
	router.SetHTMLTemplate(template.Must(template.ParseGlob("views/*.html")))

	// Create instances of repositories
	walletRepository := repository.NewWalletRepository(db.GetDB())
	coinRepository := repository.NewCoinRepository(db.GetDB())
	assetPriceRepository := repository.NewAssetPriceRepository(db.GetDB())
	historyRepository := repository.NewHistoryRepository(db.GetDB())
	orderRepository := repository.NewOrderRepository(db.GetDB())
	historyService := services.NewHistoryService(historyRepository)
	escrowRepository := repository.NewEscrowRepository(db.GetDB())
	assetBookingRepository := repository.NewAssetsBookingRepository(db.GetDB())
	revenueRepository := repository.NewRevenueRepository(db.GetDB())
	paymentSettingRepository := repository.NewPaymentSettingRepository(db.GetDB())
	// Create instances of services
	userAPIService := services.NewUserAPIService()
	revenueService := services.NewRevenueService(revenueRepository)
	walletService := services.NewWalletService(walletRepository, jwtSecretKey, userAPIService, historyService, revenueService)
	assetPriceService := services.NewAssetPriceService(assetPriceRepository)
	paymentSettingService := services.NewPaymentSettingService(paymentSettingRepository)
	// Initialize BankListService and handle potential error
	bankListService, err := services.NewBankListService()
	if err != nil {
		log.Fatalf("Error initializing BankListService: %v", err)
	}

	orderMatchingService := services.NewOrderMatchingService(orderRepository, *coinRepository, userAPIService, historyRepository, assetBookingRepository, paymentSettingRepository)
	escrowService := services.NewEscrowService(escrowRepository, orderRepository, walletService, assetBookingRepository)
	orderService := services.NewOrderService(orderRepository, walletRepository, *bankListService, escrowService, userAPIService, escrowRepository, *orderMatchingService, assetBookingRepository, *paymentSettingService)
	coinService := services.NewCoinService(
		coinRepository,
		userAPIService,
		walletService,
		*assetPriceService,
		walletRepository,
		historyService,
	)

	// Pass the JWT secret key and services to the SetupRoutes function
	routers.SetupRoutes(router, jwtSecretKey, userAPIService, walletService, coinService, orderService)

	// Start the server
	if err := router.Run(":8014"); err != nil {
		log.Fatalf("Error starting server: %v", err)
	}
	gin.SetMode(gin.ReleaseMode)
}
