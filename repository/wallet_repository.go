package repository

import (
	"wallet-app/models"
	"github.com/shopspring/decimal"
	"gorm.io/gorm"
)

type WalletRepository interface {
    FindByAssetId(assetId string) (*models.Wallet, error)
    FindByUserIdAndAsset(userId uint, cryptoId string) (*models.Wallet, error)   
    FindByWalletAddress(walletAddress string) (*models.Wallet, error)               
    DeductFromWallet(fromUserId int64, cryptoId string, amount decimal.Decimal) error            
    CreditToWallet(toUserId int64, cryptoId string, amount decimal.Decimal) error                  
	FindByUserID(userID uint) ([]models.Wallet, error)
	Create(wallets []models.Wallet) error
	WalletsExist(userID uint, cryptoIDs []string) (bool, error)
    UpdateBalances(fromUserId, toUserId int, cryptoId string, amount, feeAmount float64) error
    Update(wallet *models.Wallet) error
    Save(wallet *models.Wallet) error
    UpdateFillAmount(wallet *models.Wallet) error
}

type walletRepository struct {
	db *gorm.DB
}

// SaveOrder saves a new or existing order in the database
func (r *walletRepository) Save(wallet *models.Wallet) error {
	return r.db.Save(wallet).Error
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

// FindByUserIdAndAsset retrieves a wallet by user ID and crypto ID
func (r *walletRepository) FindByUserIdAndAsset(userID uint, cryptoID string) (*models.Wallet, error) {
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

func (r *walletRepository) FindByAssetId(assetId string) (*models.Wallet, error) {
    var wallet models.Wallet
    if err := r.db.Where("crypto_id = ?", assetId).First(&wallet).Error; err != nil {
        return nil, err
    }
    return &wallet, nil
}

// DeductFromWallet deducts the specified amount from the wallet
func (r *walletRepository) DeductFromWallet(fromUserId int64, cryptoId string, amount decimal.Decimal) error {
    return r.db.Transaction(func(tx *gorm.DB) error {
        return tx.Model(&models.Wallet{}).
            Where("user_id = ? AND crypto_id = ?", fromUserId, cryptoId).
            Update("balance", gorm.Expr("balance - ?", amount)).Error
    })
}

// CreditToWallet credits the specified amount to the wallet
func (r *walletRepository) CreditToWallet(toUserId int64, cryptoId string, amount decimal.Decimal) error {
    return r.db.Transaction(func(tx *gorm.DB) error {
        return tx.Model(&models.Wallet{}).
            Where("user_id = ? AND crypto_id = ?", toUserId, cryptoId).
            Update("balance", gorm.Expr("balance + ?", amount)).Error
    })
}

// UpdateBalances updates the balances for both the sender's and recipient's wallets
func (r *walletRepository) UpdateBalances(fromUserId, toUserId int, cryptoId string, amount, feeAmount float64) error {
    // Start a database transaction
    tx := r.db.Begin()
    if tx.Error != nil {
        return tx.Error
    }
    defer func() {
        if r := recover(); r != nil {
            tx.Rollback()
        }
    }()

    // Deduct from the sender's wallet
    updateFromWalletQuery := `UPDATE wallets SET balance = balance - ? WHERE user_id = ? AND crypto_id = ?`
    if err := tx.Exec(updateFromWalletQuery, amount+feeAmount, fromUserId, cryptoId).Error; err != nil {
        tx.Rollback()
        return err
    }

    // Credit to the recipient's wallet (net amount after fee)
    netAmount := amount - feeAmount
    updateToWalletQuery := `UPDATE wallets SET balance = balance + ? WHERE user_id = ? AND crypto_id = ?`
    if err := tx.Exec(updateToWalletQuery, netAmount, toUserId, cryptoId).Error; err != nil {
        tx.Rollback()
        return err
    }

    // Commit the transaction
    if err := tx.Commit().Error; err != nil {
        return err
    }

    return nil
}

// Update the wallet record in the database
func (repo *walletRepository) Update(wallet *models.Wallet) error {
	// Update the wallet in the database using the ID 
	if err := repo.db.Save(wallet).Error; err != nil {
		return err 
	}
	return nil
}

// Update the User wallet fIllAmount in the database
func (repo *walletRepository) UpdateFillAmount(wallet *models.Wallet) error {
	if err := repo.db.Save(wallet).Error; err != nil {
		return err 
	}
	return nil
}