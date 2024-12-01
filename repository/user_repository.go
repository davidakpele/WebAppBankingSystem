package repository

import (
    "gorm.io/gorm"
    "wallet-app/models"
)

// UserRepository defines the interface for user-related database operations
type UserRepository interface {
    FindByUsername(username string) (*models.User, error)
    // Other user-related methods
}

// userRepositoryImpl implements the UserRepository interface
type userRepositoryImpl struct {
    db *gorm.DB
}

// NewUserRepository initializes a new UserRepository
func NewUserRepository(db *gorm.DB) UserRepository {
    return &userRepositoryImpl{db: db}
}

// FindByUsername retrieves a user by username
func (r *userRepositoryImpl) FindByUsername(username string) (*models.User, error) {
    var user models.User
    if err := r.db.Where("username = ?", username).First(&user).Error; err != nil {
        return nil, err
    }
    return &user, nil
}
