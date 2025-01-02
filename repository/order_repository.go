package repository

import (
	"errors"
	"fmt"
	"time"
	"wallet-app/models"

	"github.com/shopspring/decimal"
	"gorm.io/gorm"
)

type OrderRepository interface {
	SaveOrder(order *models.Order) error
	FindByID(orderID uint) (*models.Order, error)
	FindAll() ([]models.Order, error)
	FindOrderById(orderID uint) ([]map[string]interface{}, error)
	FetchAllOrders() ([]map[string]interface{}, error)
	FindAllBuyOrdersWithPaymentSettings(orderID uint) ([]map[string]interface{}, error)
	FindSellOrdersForMatching(tradingPair string, price decimal.Decimal) ([]models.Order, error)
	FindBuyOrdersForMatching(tradingPair string, price float64) ([]models.Order, error)
	FindMatchingOrder(tradingPair string, price float64, amount float64, orderType models.OrderType) (*models.Order, error)
	UpdateOrder(order *models.Order) error
	DeleteByUserIDs(userIDs []uint) error
	DeleteByID(orderId uint) error
	GetOrdersByOrderType(orderType string) ([]map[string]interface{}, error)
	GetOrdersByUserID(userId uint) ([]map[string]interface{}, error)
}

type orderRepository struct {
	db *gorm.DB
}

// FindAll retrieves all orders from the database
func (r *orderRepository) FindAll() ([]models.Order, error) {
	var orders []models.Order
	err := r.db.Find(&orders).Error
	if err != nil {
		return nil, err
	}
	return orders, nil
}

// NewOrderRepository creates a new OrderRepository
func NewOrderRepository(db *gorm.DB) OrderRepository {
	return &orderRepository{db: db}
}

// SaveOrder saves a new or existing order in the database
func (r *orderRepository) SaveOrder(order *models.Order) error {
	return r.db.Save(order).Error
}

// FindByID retrieves an order by its ID
func (r *orderRepository) FindByID(orderID uint) (*models.Order, error) {
	var order models.Order
	err := r.db.First(&order, orderID).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, nil
	}
	return &order, err
} 

// FindAllBuyOrdersWithPaymentSettings retrieves all BUY orders and joins the PaymentSettings table.
func (r *orderRepository) FindAllBuyOrdersWithPaymentSettings(orderID uint) ([]map[string]interface{}, error) {
	var result []map[string]interface{}

	// Perform the query with a join on PaymentSettings
	err := r.db.Table("orders").
		Select(`
			orders.id AS order_id,
			orders.user_id,
			orders.is_maker,
			orders.trading_pair,
			orders.type,
			orders.price,
			orders.amount,
			orders.status,
			orders.bank_id,
			orders.expiration_time,
			orders.created_at,
			orders.updated_at,
			p.id AS payment_settings_id,
			p.order_id AS payment_settings_order_id,
			p.seller_id,
			p.remark,
			p.signature,
			p.payment_method
		`).
		Joins("LEFT JOIN order_payment_instructions ON orders.id = p.order_id").
		Where("orders.id = ?", orderID).
		Scan(&result).Error

	if err != nil {
		return nil, fmt.Errorf("failed to fetch buy orders with payment settings: %w", err)
	}

	return result, nil
}

// FindSellOrdersForMatching finds sell orders matching the trading pair and price
func (r *orderRepository) FindSellOrdersForMatching(tradingPair string, price decimal.Decimal) ([]models.Order, error) {
	var orders []models.Order
	err := r.db.Where("trading_pair = ? AND type = ? AND price = ? AND status = ?", tradingPair, models.OrderTypeSell, price, models.OrderStatusOpen).Find(&orders).Error
	return orders, err
}

// FindBuyOrdersForMatching finds buy orders matching the trading pair and price
func (r *orderRepository) FindBuyOrdersForMatching(tradingPair string, price float64) ([]models.Order, error) {
	var orders []models.Order
	err := r.db.Where("trading_pair = ? AND type = ? AND price = ? AND status = ?", tradingPair, models.OrderTypeBuy, price, models.OrderStatusOpen).Find(&orders).Error
	return orders, err
}

// FindMatchingOrder finds a matching order (buy/sell) based on trading pair, price, amount, and order type
func (r *orderRepository) FindMatchingOrder(tradingPair string, price float64, amount float64, orderType models.OrderType) (*models.Order, error) {
	var order models.Order
	err := r.db.Where("trading_pair = ? AND price = ? AND amount = ? AND status = ? AND type = ?",
		tradingPair, price, amount, models.OrderStatusPending, orderType).First(&order).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, nil // No matching order found
	}
	return &order, err
}

// UpdateOrder updates an existing order
func (r *orderRepository) UpdateOrder(order *models.Order) error {
	return r.db.Save(order).Error
}

// DeleteByUserIDs deletes orders for given user IDs
func (r *orderRepository) DeleteByUserIDs(userIDs []uint) error {
	return r.db.Where("user_id IN ?", userIDs).Delete(&models.Order{}).Error
}

// Delete By OrderId deletes 
func (r *orderRepository) DeleteByID(orderId uint) error {
	var order models.Order
	if err := r.db.Where("id = ?", orderId).First(&order).Error; err != nil {
		return fmt.Errorf("order not found")
	}

	// Delete the order
	if err := r.db.Delete(&order).Error; err != nil {
		return fmt.Errorf("failed to delete order: %v", err)
	}

	return nil
}

// Fetch orders by type using GORM
func (r *orderRepository) GetOrdersByOrderType(orderType string) ([]map[string]interface{}, error) {
	query := `
		SELECT 
			o.id AS order_id,
			o.user_id,
			o.is_maker,
			o.trading_pair,
			o.type,
			o.price,
			o.amount,
			o.status,
			o.bank_id,
			o.expiration_time,
			o.created_at,
			ps.id AS payment_settings_id,
			ps.order_id AS payment_settings_order_id,
			ps.seller_id,
			ps.remark,
			ps.signature,
			ps.payment_method
		FROM orders o
		LEFT JOIN order_payment_instructions ps ON o.id = ps.order_id WHERE o.type=?`

	rows, err := r.db.Raw(query, orderType).Rows()
	if err != nil {
		return nil, fmt.Errorf("failed to fetch orders: %w", err)
	}
	defer rows.Close()

	// Parse results
	var results []map[string]interface{}
	for rows.Next() {
		var (
			orderID           uint
			userID            uint
			isMaker           bool
			tradingPair       string
			orderType         string
			price             decimal.Decimal
			amount            decimal.Decimal
			status            string
			bankID            *uint
			expirationTime    time.Time
			createdAt         time.Time
			paymentSettingsID *uint
			paymentSettingsOrderID *uint
			sellerID          *uint
			remark            *string
			signature         *string
			paymentMethod     *string
		)

		if err := rows.Scan(
			&orderID,
			&userID,
			&isMaker,
			&tradingPair,
			&orderType,
			&price,
			&amount,
			&status,
			&bankID,
			&expirationTime,
			&createdAt,
			&paymentSettingsID,
			&paymentSettingsOrderID,
			&sellerID,
			&remark,
			&signature,
			&paymentMethod,
		); err != nil {
			return nil, fmt.Errorf("failed to scan row: %w", err)
		}

		// Add to result as a map
		results = append(results, map[string]interface{}{
			"order_id":               orderID,
			"user_id":                userID,
			"is_maker":               isMaker,
			"trading_pair":           tradingPair,
			"type":                   orderType,
			"price":                  price,
			"amount":                 amount,
			"status":                 status,
			"bank_id":                bankID,
			"expiration_time":        expirationTime,
			"created_at":             createdAt,
			"payment_settings_id":    paymentSettingsID,
			"payment_settings_order_id": paymentSettingsOrderID,
			"seller_id":              sellerID,
			"remark":                 remark,
			"signature":              signature,
			"payment_method":         paymentMethod,
		})
	}

	return results, nil
}

func (r *orderRepository) GetOrdersByUserID(userId uint) ([]map[string]interface{}, error) {
	query := `
		SELECT 
			o.id AS order_id,
			o.user_id,
			o.is_maker,
			o.trading_pair,
			o.type,
			o.price,
			o.amount,
			o.status,
			o.bank_id,
			o.expiration_time,
			o.created_at,
			ps.id AS payment_settings_id,
			ps.order_id AS payment_settings_order_id,
			ps.seller_id,
			ps.remark,
			ps.signature,
			ps.payment_method
		FROM orders o
		LEFT JOIN order_payment_instructions ps ON o.id = ps.order_id WHERE o.user_id=?`

	rows, err := r.db.Raw(query, userId).Rows()
	if err != nil {
		return nil, fmt.Errorf("failed to fetch orders: %w", err)
	}
	defer rows.Close()

	// Parse results
	var results []map[string]interface{}
	for rows.Next() {
		var (
			orderID           uint
			userID            uint
			isMaker           bool
			tradingPair       string
			orderType         string
			price             decimal.Decimal
			amount            decimal.Decimal
			status            string
			bankID            *uint
			expirationTime    time.Time
			createdAt         time.Time
			paymentSettingsID *uint
			paymentSettingsOrderID *uint
			sellerID          *uint
			remark            *string
			signature         *string
			paymentMethod     *string
		)

		if err := rows.Scan(
			&orderID,
			&userID,
			&isMaker,
			&tradingPair,
			&orderType,
			&price,
			&amount,
			&status,
			&bankID,
			&expirationTime,
			&createdAt,
			&paymentSettingsID,
			&paymentSettingsOrderID,
			&sellerID,
			&remark,
			&signature,
			&paymentMethod,
		); err != nil {
			return nil, fmt.Errorf("failed to scan row: %w", err)
		}

		// Add to result as a map
		results = append(results, map[string]interface{}{
			"order_id":               orderID,
			"user_id":                userID,
			"is_maker":               isMaker,
			"trading_pair":           tradingPair,
			"type":                   orderType,
			"price":                  price,
			"amount":                 amount,
			"status":                 status,
			"bank_id":                bankID,
			"expiration_time":        expirationTime,
			"created_at":             createdAt,
			"payment_settings_id":    paymentSettingsID,
			"payment_settings_order_id": paymentSettingsOrderID,
			"seller_id":              sellerID,
			"remark":                 remark,
			"signature":              signature,
			"payment_method":         paymentMethod,
		})
	}

	return results, nil
}

// FetchAllOrders retrieves all orders with their associated payment settings.
func (r *orderRepository) FetchAllOrders() ([]map[string]interface{}, error) {
	query := `
		SELECT 
			o.id AS order_id,
			o.user_id,
			o.is_maker,
			o.trading_pair,
			o.type,
			o.price,
			o.amount,
			o.status,
			o.bank_id,
			o.expiration_time,
			o.created_at,
			o.updated_at,
			ps.id AS payment_settings_id,
			ps.order_id AS payment_settings_order_id,
			ps.seller_id,
			ps.remark,
			ps.signature,
			ps.payment_method
		FROM orders o
		LEFT JOIN order_payment_instructions ps ON o.id = ps.order_id
	`

	rows, err := r.db.Raw(query).Rows()
	if err != nil {
		return nil, fmt.Errorf("failed to fetch orders: %w", err)
	}
	defer rows.Close()

	// Parse results
	var results []map[string]interface{}
	for rows.Next() {
		var (
			orderID           uint
			userID            uint
			isMaker           bool
			tradingPair       string
			orderType         string
			price             decimal.Decimal
			amount            decimal.Decimal
			status            string
			bankID            *uint
			expirationTime    time.Time
			createdAt         time.Time
			updatedAt         time.Time
			paymentSettingsID *uint
			paymentSettingsOrderID *uint
			sellerID          *uint
			remark            *string
			signature         *string
			paymentMethod     *string
		)

		if err := rows.Scan(
			&orderID,
			&userID,
			&isMaker,
			&tradingPair,
			&orderType,
			&price,
			&amount,
			&status,
			&bankID,
			&expirationTime,
			&createdAt,
			&updatedAt,
			&paymentSettingsID,
			&paymentSettingsOrderID,
			&sellerID,
			&remark,
			&signature,
			&paymentMethod,
		); err != nil {
			return nil, fmt.Errorf("failed to scan row: %w", err)
		}

		// Add to result as a map
		results = append(results, map[string]interface{}{
			"order_id":               orderID,
			"user_id":                userID,
			"is_maker":               isMaker,
			"trading_pair":           tradingPair,
			"type":                   orderType,
			"price":                  price,
			"amount":                 amount,
			"status":                 status,
			"bank_id":                bankID,
			"expiration_time":        expirationTime,
			"created_at":             createdAt,
			"updated_at":             updatedAt,
			"payment_settings_id":    paymentSettingsID,
			"payment_settings_order_id": paymentSettingsOrderID,
			"seller_id":              sellerID,
			"remark":                 remark,
			"signature":              signature,
			"payment_method":         paymentMethod,
		})
	}

	return results, nil
}


func (r *orderRepository) FindOrderById(orderID uint) ([]map[string]interface{}, error) {
	query := `
		SELECT 
			o.id AS order_id,
			o.user_id,
			o.is_maker,
			o.trading_pair,
			o.type,
			o.price,
			o.amount,
			o.status,
			o.bank_id,
			o.expiration_time,
			o.created_at,
			ps.id AS payment_settings_id,
			ps.order_id AS payment_settings_order_id,
			ps.seller_id,
			ps.remark,
			ps.signature,
			ps.payment_method
		FROM orders o
		LEFT JOIN order_payment_instructions ps ON o.id = ps.order_id WHERE o.id=?`

	rows, err := r.db.Raw(query, orderID).Rows()
	if err != nil {
		return nil, fmt.Errorf("failed to fetch orders: %w", err)
	}
	defer rows.Close()

	// Parse results
	var results []map[string]interface{}
	for rows.Next() {
		var (
			orderID           uint
			userID            uint
			isMaker           bool
			tradingPair       string
			orderType         string
			price             decimal.Decimal
			amount            decimal.Decimal
			status            string
			bankID            *uint
			expirationTime    time.Time
			createdAt         time.Time
			paymentSettingsID *uint
			paymentSettingsOrderID *uint
			sellerID          *uint
			remark            *string
			signature         *string
			paymentMethod     *string
		)

		if err := rows.Scan(
			&orderID,
			&userID,
			&isMaker,
			&tradingPair,
			&orderType,
			&price,
			&amount,
			&status,
			&bankID,
			&expirationTime,
			&createdAt,
			&paymentSettingsID,
			&paymentSettingsOrderID,
			&sellerID,
			&remark,
			&signature,
			&paymentMethod,
		); err != nil {
			return nil, fmt.Errorf("failed to scan row: %w", err)
		}

		// Add to result as a map
		results = append(results, map[string]interface{}{
			"order_id":               orderID,
			"user_id":                userID,
			"is_maker":               isMaker,
			"trading_pair":           tradingPair,
			"type":                   orderType,
			"price":                  price,
			"amount":                 amount,
			"status":                 status,
			"bank_id":                bankID,
			"expiration_time":        expirationTime,
			"created_at":             createdAt,
			"payment_settings_id":    paymentSettingsID,
			"payment_settings_order_id": paymentSettingsOrderID,
			"seller_id":              sellerID,
			"remark":                 remark,
			"signature":              signature,
			"payment_method":         paymentMethod,
		})
	}

	return results, nil
}
