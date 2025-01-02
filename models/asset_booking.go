package models

type Bookings struct {
	ID       uint  `gorm:"primaryKey"`
	OrderID  uint  `gorm:"not null"`
	BuyerID  uint  `gorm:"not null"`
	SellerID uint  `gorm:"not null"`
}

// TableName overrides the default table name
func (Bookings) TableName() string {
	return "bookings"
}
