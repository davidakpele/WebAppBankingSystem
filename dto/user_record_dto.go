package dto

import "time"

type UserRecordDTO struct {
	ID             int64     `json:"id"`
	FirstName      string    `json:"firstName"`
	LastName       string    `json:"lastName"`
	Gender         string    `json:"gender"`
	IsTransferPin  bool      `json:"isTransferPin"`
	Locked         bool      `json:"locked"`
	LockedAt       time.Time `json:"lockedAt,omitempty"`
	ReferralCode   string    `json:"referralCode"`
	IsBlocked      bool      `json:"isBlocked"`
	BlockedDuration int64    `json:"blockedDuration,omitempty"`
	BlockedUntil   string    `json:"blockedUntil,omitempty"`
	BlockedReason  string    `json:"blockedReason,omitempty"`
	TotalRefs      string    `json:"totalRefs,omitempty"`
	Notifications  string    `json:"notifications,omitempty"`
	ReferralLink   string    `json:"referralLink,omitempty"`
	Photo          string    `json:"photo,omitempty"`
}
