// db/db.go

package db

import (
    "fmt"
    "log"
    "os"
    "github.com/joho/godotenv"
    "gorm.io/driver/mysql"
    "gorm.io/gorm"
    "wallet-app/models" 
)

var DB *gorm.DB

// Connect initializes the database connection
func Connect() {
    // Load .env file
    err := godotenv.Load()
    if err != nil {
        log.Fatalf("Error loading .env file")
    }

    // Get environment variables
    host := os.Getenv("DB_HOST")
    user := os.Getenv("DB_USER")
    password := os.Getenv("DB_PASSWORD")
    dbname := os.Getenv("DB_NAME")
    port := os.Getenv("DB_PORT")

    // Build the connection string for MySQL
    dsn := fmt.Sprintf("%s:%s@tcp(%s:%s)/%s?parseTime=true&loc=Local",
        user, password, host, port, dbname)

    // Connect to the database
    DB, err = gorm.Open(mysql.Open(dsn), &gorm.Config{})
    if err != nil {
        log.Fatalf("Could not connect to the database: %v", err)
    }
    log.Println("Database connection established")

    // Run migrations
    err = DB.AutoMigrate(
        &models.Wallet{},
        &models.Coin{},
        &models.Bookings{},
        &models.Revenue{},
        &models.History{},
        &models.Order{},
        &models.PaymentSettings{},
        &models.Escrow{}) 
    if err != nil {
        log.Fatalf("Could not migrate the database: %v", err)
    }
    log.Println("Database migrated successfully")
}

// GetDB returns the current database instance
func GetDB() *gorm.DB {
    return DB
}
