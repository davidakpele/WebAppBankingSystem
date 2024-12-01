package models

import (
	"time"
	"github.com/jinzhu/gorm"
)

type Escrow struct {
	gorm.Model
	ID        uint        `gorm:"primary_key" json:"id"`
	OrderID   uint        `json:"order_id"`
	Amount    float64     `gorm:"type:decimal(19,4)" json:"amount"`
	Status    OrderStatus `gorm:"type:varchar(50)" json:"status"`
	CreatedAt time.Time   `json:"created_at"`
}
