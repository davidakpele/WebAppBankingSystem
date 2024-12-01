package models

import (
	"github.com/jinzhu/gorm"
)

type CoinBooking struct {
	gorm.Model
	ID      uint `gorm:"primary_key" json:"id"`
	OrderID uint `json:"order_id"`
	BuyerID uint `json:"buyer_id"`
	SellerID uint `json:"seller_id"`
}
