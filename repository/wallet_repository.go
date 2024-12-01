package repository

import (
	"wallet-app/models"
	"gorm.io/gorm"
    "math/big"
)

type WalletRepository interface {
    FindByUserIdAndCryptoId(userId uint, cryptoId string) (*models.Wallet, error)   // Find wallet by user ID and crypto ID
    FindByWalletAddress(walletAddress string) (*models.Wallet, error)               // Find wallet by wallet address
    DeductFromWallet(wallet *models.Wallet, amount *big.Float) error                // Deduct amount from wallet
    CreditToWallet(wallet *models.Wallet, amount *big.Float) error                  // Credit amount to wallet
	FindByUserID(userID uint) ([]models.Wallet, error)
	Create(wallets []models.Wallet) error
	WalletsExist(userID uint, cryptoIDs []string) (bool, error)
}

type walletRepository struct {
	db *gorm.DB
}

func NewWalletRepository(db *gorm.DB) WalletRepository {
	return &walletRepository{db: db}
}

func (r *walletRepository) FindByUserID(userID uint) ([]models.Wallet, error) {
	var wallets []models.Wallet
	err := r.db.Where("user_id = ?", userID).Find(&wallets).Error
	if err != nil {
		return nil, err
	}
	return wallets, nil
}

func (r *walletRepository) Create(wallets []models.Wallet) error {
	return r.db.Create(&wallets).Error
}

func (r *walletRepository) WalletsExist(userID uint, cryptoIDs []string) (bool, error) {
	var count int64
	if err := r.db.Model(&models.Wallet{}).
		Where("user_id = ? AND crypto_id IN ?", userID, cryptoIDs).
		Count(&count).Error; err != nil {
		return false, err
	}
	return count > 0, nil
}


// FindByUserIdAndCryptoId retrieves a wallet by user ID and crypto ID
func (r *walletRepository) FindByUserIdAndCryptoId(userID uint, cryptoID string) (*models.Wallet, error) {
    var wallet models.Wallet
    if err := r.db.Where("user_id = ? AND crypto_id = ?", userID, cryptoID).First(&wallet).Error; err != nil {
        return nil, err
    }
    return &wallet, nil
}


// FindByWalletAddress retrieves a wallet by its address
func (r *walletRepository) FindByWalletAddress(walletAddress string) (*models.Wallet, error) {
    var wallet models.Wallet
    if err := r.db.Where("wallet_address = ?", walletAddress).First(&wallet).Error; err != nil {
        return nil, err
    }
    return &wallet, nil
}

// DeductFromWallet deducts the specified amount from the wallet
func (r *walletRepository) DeductFromWallet(wallet *models.Wallet, amount *big.Float) error {
    // Logic to deduct amount from the wallet
    // You might need to convert amount to a format suitable for your database
    // Update the wallet balance accordingly
    return r.db.Model(wallet).Update("balance", gorm.Expr("balance - ?", amount)).Error
}

// CreditToWallet credits the specified amount to the wallet
func (r *walletRepository) CreditToWallet(wallet *models.Wallet, amount *big.Float) error {
    // Logic to credit amount to the wallet
    // Update the wallet balance accordingly
    return r.db.Model(wallet).Update("balance", gorm.Expr("balance + ?", amount)).Error
}