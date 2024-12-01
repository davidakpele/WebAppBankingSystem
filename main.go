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
	userRepository := repository.NewUserRepository(db.GetDB())
	walletRepository := repository.NewWalletRepository(db.GetDB()) // Correct repository initialization
	revenueService := repository.NewRevenueService(db.GetDB())

	// Create instances of services, passing the repository and JWT secret
	userService := services.NewUserService(userRepository, walletRepository, revenueService)
	walletService := services.NewWalletService(walletRepository, jwtSecretKey) // Corrected service initialization

	// Pass the JWT secret key and services to the SetupRoutes function
	routers.SetupRoutes(router, jwtSecretKey, userService, walletService)

	// Start the server
	if err := router.Run(":8014"); err != nil {
		log.Fatalf("Error starting server: %v", err)
	}
}
