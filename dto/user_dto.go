package dto

type UserDTO struct {
	ID        int64           `json:"id"`
	Email     string          `json:"email"`
	Username  string          `json:"username"`
	Enabled   bool            `json:"enabled"`
	Records   []UserRecordDTO `json:"records,omitempty"` // Omit if empty
	Role      string          `json:"role,omitempty"`    // Omit if empty
}
