package utils

import (
	"crypto/rand"
	"encoding/hex"
	"errors"
	"fmt"
	"github.com/blocto/solana-go-sdk/types"
	"github.com/btcsuite/btcd/btcec/v2"
	"github.com/btcsuite/btcd/btcutil"
	"github.com/btcsuite/btcd/chaincfg"
	"github.com/ethereum/go-ethereum/crypto"
	"golang.org/x/crypto/ed25519"
)

func CreateWalletAddressForCrypto(crypto string) (string, error) {
	switch crypto {
        case "Bitcoin":
            return createBitcoinWallet()
        case "Ethereum", "Shiba Inu", "Polygon":
            return createEthereumWallet()
        case "Solana":
            return createSolanaWallet()
        case "Litecoin":
            return createLitecoinWallet()
        case "Dogecoin":
            return createDogecoinWallet()
        case "Tron":
            return createTronWallet()
        case "Ripple":
            return createRippleWallet()
        case "Binance":
            return createBinanceWallet()
        case "Cardano":
            return createCardanoWallet()
        case "Dash":
            return createDashWallet()
        default:
		return "", errors.New(fmt.Sprintf("unsupported cryptocurrency: %s", crypto))
	}
}


// createBitcoinWallet creates a Bitcoin wallet and returns its address and WIF
func createBitcoinWallet() (string, error) {
    privateKey, err := btcec.NewPrivateKey()
    if err != nil {
        return "", err
    }
    addressPubKey, err := btcutil.NewAddressPubKey(privateKey.PubKey().SerializeCompressed(), &chaincfg.MainNetParams)
    if err != nil {
        return "", err
    }
    return addressPubKey.EncodeAddress(), nil
}

// createEthereumWallet creates an Ethereum wallet and returns its address and private key in hex
func createEthereumWallet() (string, error) {
    privateKey, err := crypto.GenerateKey()
    if err != nil {
        return "", err
    }
    
    address := crypto.PubkeyToAddress(privateKey.PublicKey).Hex()
    return address, nil
}

// createSolanaWallet creates a Solana wallet and returns its address and private key in hex
func createSolanaWallet() (string, error) {
    account := types.NewAccount()
    return account.PublicKey.ToBase58(), nil
}

// createRippleWallet creates a Ripple wallet and returns its address and secret in hex
func createRippleWallet() (string, error) {
    publicKey, _, err := ed25519.GenerateKey(rand.Reader) 
    if err != nil {
        return "", err
    }
    address := hex.EncodeToString(publicKey)
    return address, nil
}


// createLitecoinWallet creates a Litecoin wallet and returns its address and WIF
func createLitecoinWallet() (string, error) {
    privateKey, err := btcec.NewPrivateKey()
    if err != nil {
        return "", err
    }
    addressPubKey, err := btcutil.NewAddressPubKey(privateKey.PubKey().SerializeCompressed(), &chaincfg.MainNetParams)
    if err != nil {
        return "", err
    }
    return addressPubKey.EncodeAddress(), nil
}

// createDogecoinWallet creates a Dogecoin wallet and returns its address and WIF
func createDogecoinWallet() (string, error) {
    privateKey, err := btcec.NewPrivateKey()
    if err != nil {
        return "", err
    }
    addressPubKey, err := btcutil.NewAddressPubKey(privateKey.PubKey().SerializeCompressed(), &chaincfg.MainNetParams)
    if err != nil {
        return "", err
    }
    return addressPubKey.EncodeAddress(), nil
}

// createBinanceWallet creates a Binance Smart Chain wallet and returns its address and private key in hex
func createBinanceWallet() (string, error) {
    privateKey, err := crypto.GenerateKey()
    if err != nil {
        return "", err
    }
    address := crypto.PubkeyToAddress(privateKey.PublicKey).Hex()
    return address, nil
}

// createTronWallet creates a Tron wallet and returns its address and private key in hex
func createTronWallet() (string, error) {
    publicKey, _, err := ed25519.GenerateKey(rand.Reader)
    if err != nil {
        return "", err
    }
    address := hex.EncodeToString(publicKey)
    return address, nil
}

// createDashWallet creates a Dash wallet (mock implementation, adapt as needed)
func createDashWallet() (string, error) {
    privateKey, err := btcec.NewPrivateKey()
    if err != nil {
        return "", err
    }
    addressPubKey, err := btcutil.NewAddressPubKey(privateKey.PubKey().SerializeCompressed(), &chaincfg.MainNetParams)
    if err != nil {
        return "", err
    }
    return addressPubKey.EncodeAddress(), nil
}

// createCardanoWallet creates a Cardano wallet (mock implementation, adapt as needed)
func createCardanoWallet() (string,  error) {
    publicKey := make([]byte, 32)
    _, err := rand.Read(publicKey)
    if err != nil {
        return "", err
    }
    address := hex.EncodeToString(publicKey) 
    return address, nil
}

// func generateKeyPair() (*rsa.PrivateKey, *rsa.PublicKey) {
//     privateKey, err := rsa.GenerateKey(rand.Reader, 2048)
//     if err != nil {
//         fmt.Println(err)
//         os.Exit(1)
//     }
//     publicKey := &privateKey.PublicKey
//     return privateKey, publicKey
// }

// func encryptData(publicKey *rsa.PublicKey, data []byte) ([]byte, error) {
//     encryptedData, err := rsa.EncryptPKCS1v15(rand.Reader, publicKey, data)
//     if err != nil {
//         return nil, err
//     }
//     return encryptedData, nil
// }

// func signData(privateKey *rsa.PrivateKey, data []byte) ([]byte, error) {
//     signedData, err := rsa.SignPKCS1v15(rand.Reader, privateKey, crypto.SHA256, data)
//     if err != nil {
//         return nil, err
//     }
//     return signedData, nil
// }

// func verifySignature(publicKey *rsa.PublicKey, signedData []byte, data []byte) error {
//     return rsa.VerifyPKCS1v15(publicKey, crypto.SHA256, data, signedData)
// }



// func main() {
//     // Generate a key pair
//     privateKey, publicKey := generateKeyPair()

//     // Encrypt user details
//     userData := []byte("John Doe")
//     encryptedData, err := encryptData(publicKey, userData)
//     if err != nil {
//         fmt.Println(err)
//         os.Exit(1)
//     }

//     // Sign the encrypted data
//     signedData, err := signData(privateKey, encryptedData)
//     if err != nil {
//         fmt.Println(err)
//         os.Exit(1)
//     }

//     // Verify the signature
//     err = verifySignature(&publicKey, signedData, encryptedData)
//     if err != nil {
//         fmt.Println(err)
//         os.Exit(1)
//     }

//     fmt.Println("Signature verified successfully!")
// }