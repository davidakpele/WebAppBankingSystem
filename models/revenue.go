package models

import (
	"time"

	"github.com/jinzhu/gorm"
	"github.com/shopspring/decimal"
)

type Revenue struct {
	gorm.Model
	ID        uint      `gorm:"primaryKey;autoIncrement" json:"id"`
	Balance   decimal.Decimal `gorm:"type:decimal(19,2)" json:"balance"`
	UpdatedAt time.Time `gorm:"not null;autoUpdateTime" json:"updated_at"`
}
