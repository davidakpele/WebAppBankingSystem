package utils

import (
	"encoding/json"
	"fmt"
	"math/big"
)

type BigInt struct {
	*big.Int
}

// UnmarshalJSON handles JSON deserialization for BigInt.
func (b *BigInt) UnmarshalJSON(data []byte) error {
	str := string(data)
	// Trim quotes (e.g., "12345" -> 12345)
	str = str[1 : len(str)-1] 
	b.Int = new(big.Int)
	_, ok := b.SetString(str, 10)
	if !ok {
		return fmt.Errorf("invalid big integer: %s", str)
	}
	return nil
}

// MarshalJSON handles JSON serialization for BigInt.
func (b BigInt) MarshalJSON() ([]byte, error) {
	return json.Marshal(b.String())
}
