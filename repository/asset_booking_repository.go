package repository

import (
	"errors"
	"wallet-app/models"

	"gorm.io/gorm"
)

type AssetsBookingRepository interface {
	FindByOrderID(orderID uint) (*models.Bookings, error)
	DeleteByUserIDs(userIDs []uint) error
	DeleteByID(bookingID uint) error
	DeleteOrderByOrderID(orderId uint) error 
	SaveBooking(booking *models.Bookings) error
}

type assetsBookingRepository struct {
	db *gorm.DB
}

func NewAssetsBookingRepository(db *gorm.DB) AssetsBookingRepository {
	return &assetsBookingRepository{db: db}
}

// FindByOrderID retrieves a CoinBooking by its associated order ID.
func (r *assetsBookingRepository) FindByOrderID(orderID uint) (*models.Bookings, error) {
	var coinBooking models.Bookings
	err := r.db.Where("order_id = ?", orderID).First(&coinBooking).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, nil
	}
	return &coinBooking, err
}

// DeleteByUserIDs deletes CoinBookings associated with a list of buyer or seller IDs.
func (r *assetsBookingRepository) DeleteByUserIDs(userIDs []uint) error {
	err := r.db.Where("buyer_id IN ? OR seller_id IN ?", userIDs, userIDs).Delete(&models.Bookings{}).Error
	return err
}

// DeleteByID deletes a CoinBooking by its ID.
func (r *assetsBookingRepository) DeleteByID(bookingID uint) error {
	err := r.db.Where("id = ?", bookingID).Delete(&models.Bookings{}).Error
	return err
}

// Delete Booking By Order ID deletes a CoinBooking by its ID.
func (r *assetsBookingRepository) DeleteOrderByOrderID(orderId uint) error {
	err := r.db.Where("order_id = ?", orderId).Delete(&models.Bookings{}).Error
	return err
}

// Save Booking saves a new or existing Booking in the database
func (r *assetsBookingRepository) SaveBooking(booking *models.Bookings) error {
	return r.db.Create(booking).Error
}

