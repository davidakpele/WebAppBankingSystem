package dto

type UserRecordDTO struct {
	ID              int64      `json:"id"`
	FirstName       string     `json:"firstName"`
	LastName        string     `json:"lastName"`
	Gender          string     `json:"gender"`
	IsTransferPin   bool       `json:"_transfer_pin"`
	Locked          bool       `json:"locked"`       // Omit if nil
	ReferralCode    string     `json:"referralCode,omitempty"`    // Omit if empty
	IsBlocked       bool       `json:"blocked"`
	BlockedDuration *int64     `json:"blockedDuration,omitempty"` // Omit if nil
	BlockedUntil    *string    `json:"blockedUntil,omitempty"`    // Omit if nil
	BlockedReason   *string    `json:"blockedReason,omitempty"`   // Omit if nil
	TotalRefs       *string    `json:"totalRefs,omitempty"`       // Omit if nil
	Notifications   *string    `json:"notifications,omitempty"`  // Omit if nil
	ReferralLink    string     `json:"referralLink,omitempty"`    // Omit if empty
	Photo           *string    `json:"photo,omitempty"`           // Omit if nil
}
