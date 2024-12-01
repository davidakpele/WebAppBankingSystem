package dto

import (
	"time"
)

type UserDTO struct {
	ID        int64               `json:"id"`
	Email     string              `json:"email"`
	Username  string              `json:"username"`
	Password  string              `json:"password"`
	CreatedOn time.Time           `json:"createdOn"`
	UpdatedOn time.Time           `json:"updatedOn"`
	Enabled   bool                `json:"enabled"`
	Records   []UserRecordDTO     `json:"records"`
	Role      string              `json:"role"` 
}

// Methods to emulate `UserDetails` behavior
func (u *UserDTO) GetAuthorities() []string {
	// Replace this with real role-handling logic if necessary
	return []string{u.Role}
}

func (u *UserDTO) GetPassword() string {
	return u.Password
}

func (u *UserDTO) GetUsername() string {
	return u.Username
}

func (u *UserDTO) IsAccountNonExpired() bool {
	return true
}

func (u *UserDTO) IsAccountNonLocked() bool {
	return true
}

func (u *UserDTO) IsCredentialsNonExpired() bool {
	return true
}

func (u *UserDTO) IsEnabled() bool {
	return u.Enabled
}
