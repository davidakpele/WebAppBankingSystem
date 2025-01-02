package services

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"os"
	"strings"
	"time"
	"wallet-app/dto"
	"wallet-app/models"
	"wallet-app/payloads"
	"wallet-app/repository"
	"wallet-app/responses"
	"wallet-app/security"

	"github.com/gin-gonic/gin"
)

// OrderService defines the interface for the order service
type OrderService interface {
	CreateOrder(userID uint, request payloads.OrderRequest, r *http.Request) (*models.Order, error)
	FindById(orderID uint) (*models.Order, error)
	FetchOrderByOrderId(orderID uint, c *gin.Context) ([]map[string]interface{}, error)
	FindAllOrdersWithPaymentSettings(orderID uint)  ([]map[string]interface{}, error) 
	FetchAllOrders(c *gin.Context) ([]map[string]interface{}, error)
	UpdateOrder(orderID uint, updatedOrder models.Order) (*models.Order, error)
	CancelOrder(orderID uint) error
	HandleP2POrderAction(orderID uint, userID uint) error
	DeleteOrderByOrderID(orderID uint, userID uint) error
	UpdateOrderStatusForTradeCompletion(orderID uint, userID uint) (*models.Order, error)
    GetOrdersByOrderType(orderType string, c *gin.Context) ([]map[string]interface{}, error)
    GetBankDetailsByUserID(userID uint, bankIDs []uint, jwt string) ([]dto.BankListDTO, error) 
    GetUserOrderList(userID uint, c *gin.Context)  ([]map[string]interface{}, error)
}

type LocalBankDetailsDTO struct {
    Order       models.Order    `json:"order"`
    BankDetails []dto.BankListDTO `json:"bank_details"`
}

type NotFoundError struct {
    Message string `json:"message"`
    Code    int    `json:"code"`
}

// orderService is the implementation of OrderService
type orderService struct {
	orderRepo  repository.OrderRepository
	walletRepo repository.WalletRepository 
	banklistService BankListService
	escrowService  EscrowService
	userService    UserAPIService
    escrowRepo repository.EscrowRepository
	orderMatchingService OrderMatchingService
    bookingRepo repository.AssetsBookingRepository
	paymentSettingsService PaymentSettingService
    httpClient *http.Client
}

// NewOrderService initializes and returns an instance of OrderService
func NewOrderService(orderRepo repository.OrderRepository, walletRepo repository.WalletRepository, banklistService BankListService, escrowService EscrowService, userService UserAPIService, escrowRepo repository.EscrowRepository, orderMatchingService OrderMatchingService, bookingRepo repository.AssetsBookingRepository, paymentSettingsService PaymentSettingService) OrderService {
    return &orderService{
        orderRepo:     orderRepo,
        walletRepo:    walletRepo,
        banklistService: banklistService,
        escrowService: escrowService,
		userService: userService,
        escrowRepo: escrowRepo,
		orderMatchingService: orderMatchingService,
        bookingRepo: bookingRepo,
		paymentSettingsService: paymentSettingsService,
        httpClient: &http.Client{Timeout: 10 * time.Second},
    }
}

// Create custom NotFoundError Message handler
func (e *NotFoundError) Error() string {
    return e.Message
}

func (s *orderService) CreateOrder(userID uint, request payloads.OrderRequest, r *http.Request) (*models.Order, error) {
	// Step 1: Extract token
    authHeader := r.Header.Get("Authorization")
    if authHeader == "" {
		return nil, responses.CreateErrorResponse("Authorization header is missing", "")
    }

    // 2 Split the header value into "Bearer" and the token
    parts := strings.Split(authHeader, " ")
    if len(parts) != 2 || strings.ToLower(parts[0]) != "bearer" {
		return nil, responses.CreateErrorResponse("invalid authorization header format", "")
    }

	fromUser, err := s.userService.FindByUserId(request.UserID)
	if err != nil {
        return nil, responses.CreateErrorResponse("User not found", err.Error())
    }
	
	if fromUser.ID != int64(userID){
		return nil, responses.CreateErrorResponse("Unauthorized Action", "You are not authorized to operate this wallet.")
	}

    token := parts[1]
	// Step 3: Fetch Bank List
	bankDetails, err := s.banklistService.GetBankDetails(uint(fromUser.ID), request.BankID, token)
	
    if err != nil {
        return nil, responses.CreateErrorResponse("Bank details does not match the system.", err.Error())
    }

	// Step 4: Fetch User's Wallet
    fromWallet, err := s.walletRepo.FindByUserIdAndAsset(userID, request.TradingPair)
    if err != nil {
        return nil, err
    }
	var bankID *uint
    // Step 5: Check if Order Type is SELL and Balance is Sufficient
    if request.Type == string(models.OrderTypeSell) {
        if fromWallet.Balance.Cmp(request.Amount) < 0 {
            return nil, responses.CreateErrorResponse("Sorry you do not have enough crypto balance to complete this transaction.", err.Error())
        }
        fromWallet.Balance = fromWallet.Balance.Sub(request.Amount)
		bankID = &bankDetails.ID 
        // Save updated wallet
        err := s.walletRepo.Update(fromWallet)
        if err != nil {
            return nil, err
        }
    } else {
		bankID = nil // Use nil
	}
    // Step 6: Create Order
    order := &models.Order{
        UserID:       userID,
        TradingPair:  request.TradingPair,
        Type:         models.OrderType(request.Type),
        Price:        request.Price,
        Amount:       request.Amount,
        Status:       models.OrderStatusOpen,
        BankID:       bankID,
        CreatedAt:    time.Now(),
        UpdatedAt:    time.Now(),
    }

    // Save Order 
    err = s.orderRepo.SaveOrder(order)
    if err != nil {
        return nil, responses.CreateErrorResponse("failed to save order: %w", err.Error())
    }

	var paymentType models.PaymentMethodType
    // Step7: Create Escrow if SELL Order
    if request.Type == string(models.OrderTypeSell) {
		paymentType = models.PaymentMethodType(request.PaymentMethod)
		// 7a Create Escrow 
        err = s.escrowService.CreateEscrow(order, order.Amount)
        if err != nil {
            return nil, err
        }

		// 7b Create Payment Instructions
		err = s.paymentSettingsService.Create(order.ID, request.UserID, request.Remark, request.Signature, paymentType)
		if err != nil {
            return nil, err
        }
    }else{
		paymentType = ""
	}

    // Step 8: Match the Order 
	ctx := context.Background()
	err = s.orderMatchingService.MatchOrders(ctx, order, paymentType)
	if err != nil {
		return nil, err
	}

    // Return Created Order
    return order, nil
}

// Find order by order Id
func (s *orderService) FindById(orderID uint) (*models.Order, error) {
	order, err := s.orderRepo.FindByID(orderID)
    if err != nil {
        // Return error as is
        return nil, err
    }
    
    // Check if order is found
    if order == nil {
        // Return JSON error response
        return nil, &NotFoundError{
            Message: "Order not found",
            Code:    http.StatusNotFound,
        }
    }
    
    // Return found order
    return order, nil
}

// GetAllBuyOrdersWithPaymentSettings retrieves all BUY orders with their corresponding payment settings.
func (s *orderService) FetchOrderByOrderId(orderID uint, c *gin.Context) ([]map[string]interface{}, error) {
    // Fetch data from the repository
    ordersWithSettings, err := s.orderRepo.FindOrderById(orderID)
    if err != nil {
        return nil, err
    }

    // Check if order is found
    if ordersWithSettings == nil {
        return nil, &NotFoundError{
            Message: "Order not found",
            Code:    http.StatusNotFound,
        }
    }

    // Extract jwt token from header request
    token, err := security.ExtractHeaderToken(c)
    if err != nil {
        return nil, err
    }

    var updatedOrdersWithSettings []map[string]interface{}
    for _, order := range ordersWithSettings {
		updatedOrder := make(map[string]interface{})
		for k, v := range order {
			updatedOrder[k] = v
		}

		// Check order type
		orderType, ok := order["type"]
		if !ok {
			return nil, &NotFoundError{
				Message: "Order type is missing",
				Code:    http.StatusNotFound,
			}
		}

		// If order type is BUY, skip processing
		if orderType == string(models.OrderTypeBuy){
			updatedOrdersWithSettings = append(updatedOrdersWithSettings, updatedOrder)
			continue
		}

		// If order type is SELL, process bank details
		if orderType == string(models.OrderTypeSell){
			// Extract bankId and userId
			bankId, ok := order["bank_id"]
			if !ok || bankId == nil {
				return nil, &NotFoundError{
					Message: "Bank ID is missing",
					Code:    http.StatusNotFound,
				}
			}

			userId, ok := order["user_id"]
			if !ok {
				return nil, &NotFoundError{
					Message: "User ID is missing",
					Code:    http.StatusNotFound,
				}
			}

			userIdValue, ok := userId.(uint)
			if !ok {
				return nil, &NotFoundError{
					Message: "User ID is not a valid uint",
					Code:    http.StatusNotFound,
				}
			}

			bankIdValue, ok := bankId.(*uint)
			if !ok {
				return nil, &NotFoundError{
					Message: "Bank ID is not a valid uint",
					Code:    http.StatusNotFound,
				}
			}

			// Fetch bank details
			bankDetails, err := s.banklistService.GetBankDetails(userIdValue, *bankIdValue, token)
			if err != nil {
				return nil, responses.CreateErrorResponse("Bank details does not match the system.", err.Error())
			}

			// Append bank details to the order
			updatedOrder["bank_details"] = bankDetails
			updatedOrdersWithSettings = append(updatedOrdersWithSettings, updatedOrder)
		}
	}

    return updatedOrdersWithSettings, nil
}

// GetOrderByID retrieves an order by its ID
func (s *orderService) FindAllOrdersWithPaymentSettings(orderID uint)  ([]map[string]interface{}, error) {
	order, err := s.orderRepo.FindAllBuyOrdersWithPaymentSettings(orderID)
    if err != nil {
        // Return error as is
        return nil, err
    }
    
    // Check if order is found
    if order == nil {
        // Return JSON error response
        return nil, &NotFoundError{
            Message: "Order not found",
            Code:    http.StatusNotFound,
        }
    }
    
    // Return found order
    return order, nil
}

// GetAllOrders retrieves all orders
func (s *orderService) GetAllOrders() ([]models.Order, error) {
	return s.orderRepo.FindAll()
}

// UpdateOrder updates an existing order
func (s *orderService) UpdateOrder(orderID uint, updatedOrder models.Order) (*models.Order, error) {
	// Retrieve the existing order
	order, err := s.FindById(orderID)
	if err != nil {
		return nil, err
	}

	// Update the fields of the existing order
	order.Amount = updatedOrder.Amount
	order.Price = updatedOrder.Price
	order.UpdatedAt = time.Now()

	// Save the updated order
	err = s.orderRepo.SaveOrder(order)
	if err != nil {
		return nil, err
	}

	return order, nil
}

// CancelOrder cancels an order
func (s *orderService) CancelOrder(orderID uint) error {
	// Step 1: Retrieve the order by ID
	order, err := s.FindById(orderID)
	if err != nil {
		return err
	}

	// Step 2: Check if the order type is SELL
	if order.Type == models.OrderTypeSell {
		// Step 3: Find the escrow by order ID
		escrow, err := s.escrowRepo.FindByOrderID(orderID)
		if err != nil {
			return err
		}

		// Step 4: If escrow exists, find the wallet using the trading pair of the order
		if escrow != nil {
			coinwallet, err := s.walletRepo.FindByAssetId(order.TradingPair)
			if err != nil {
				return err
			}

			// Step 5: Update the wallet balance by adding the escrow amount
			if coinwallet != nil {
				coinwallet.Balance = coinwallet.Balance.Add(escrow.Amount) 
				err = s.walletRepo.Save(coinwallet)  
				if err != nil {
					return err
				}
			}

			// Step 6: Delete the escrow record
			escrow.Status = models.EscrowStatusCancelled
			err = s.escrowRepo.Update(escrow) 
			if err != nil {
				return err
			}
		}
	}

	// Step 7: Set the order status to CANCELLED and save the order
	order.Status = models.OrderStatusCancelled
	err = s.orderRepo.UpdateOrder(order) 
	return err
}

// HandleP2POrderAction handles actions for peer-to-peer orders
func (s *orderService) HandleP2POrderAction(orderID uint, userID uint) error {
    // Retrieve the order
    order, err := s.FindById(orderID)
    if err != nil {
        return responses.CreateErrorResponse("Order not found", "Order not found")
    }

    // Check if the order is canceled
    if order.Status == models.OrderStatusCancelled {
        return responses.CreateErrorResponse("Order is not available for trade", "Could not find the order with ID: " + fmt.Sprintf("%d", orderID))
    }

    // Check if the order is not open
    if order.Status != models.OrderStatusOpen {
        return responses.CreateErrorResponse("Order is not available for trade", "The order is either filled or canceled.")
    }

    // Check if the user is the owner of the order
    if order.UserID == userID {
        return responses.CreateErrorResponse("You cannot purchase your own product", "You can't place an order on any of your own orders. It is considered illegal. We advise you cancel your order if you can't continue.")
    }

    // Determine if the action is a buy or sell
    isBuyerAction := order.Type == models.OrderTypeBuy
    var buyerID, sellerID uint

    if isBuyerAction {
        order.Status = models.OrderStatusPending
        buyerID = userID
        sellerID = order.UserID
    } else {
        order.Status = models.OrderStatusFilled
        buyerID = userID
        sellerID = order.UserID
    }

    // Create a coin booking
    coinBooking := &models.Bookings{
        OrderID: order.ID,
        BuyerID: buyerID,
        SellerID: sellerID,
    }

    // Save updated order
    err = s.orderRepo.SaveOrder(order)
    if err != nil {
        return nil
    }

    // Save the coin booking
    err = s.bookingRepo.SaveBooking(coinBooking)
    if err != nil {
        return nil
    }

    // Additional logic for sending emails can be added here

    return nil
}

// orderService - Service layer handling order-related operations.
func (s *orderService) DeleteOrderByOrderID(orderID uint, userID uint) error {
	// Check if orderID is 0 (empty or invalid)
	if orderID == 0 {
		return responses.CreateErrorResponse("Invalid Order ID", "Order ID cannot be empty or zero.")
	}

	// Retrieve the order by ID
	order, err := s.FindById(orderID)
	if err != nil {
		return err
	}

	// Check if the order belongs to the user (optional check)
	if order.UserID != userID {
		return responses.CreateErrorResponse("Unauthorized", "You can only delete your own orders.")
	}

	// Check if order has been BOOKED | FILLED | PAYMENT_PENDING
	
	if order.Status == models.OrderStatusFilled || order.Status == models.OrderStatusPartiallyFilled || order.Status == models.OrderStatus(models.EscrowStatusPartiallyFilled) || order.Status == models.OrderStatus(models.EscrowStatusFilled) || order.Status == models.OrderStatus(models.EscrowStatusPending) {
		return responses.CreateErrorResponse("You can't delete this order because it already in transaction process", "")
	}
	
	// delete Escrow by Order Id
	err = s.escrowRepo.DeleteByOrderID(orderID)
	if err != nil {
		return err
	}
	// delete Booking by Order Id
	err = s.bookingRepo.DeleteOrderByOrderID(orderID)
	if err != nil {
		return err
	}
	// delete Order by Order Id
	err = s.orderRepo.DeleteByID(orderID)
	if err != nil {
		return err
	}

	// delete Order by Order Id
	err = s.paymentSettingsService.DeleteByOrderIdAndUserId(orderID, order.UserID)
	if err != nil {
		return err
	}
	// you can add a success response or log the deletion
	return nil
}

// UpdateOrderStatusForTradeCompletion updates the status of an order for trade completion
func (s *orderService) UpdateOrderStatusForTradeCompletion(orderID uint, userID uint) (*models.Order, error) {
	order, err := s.FindById(orderID)
	if err != nil {
		return nil, err
	}

	if order.UserID != userID {
		return nil, errors.New("you are not authorized to operate on this order")
	}

	// Handle account lock/block checks here

	order.Status = models.OrderStatusPaymentVerified
	// Only capture the error returned by SaveOrder
	err = s.orderRepo.SaveOrder(order)

	// Return the order and the error (if any)
	return order, err
}

// Fetch orders by type
func (s *orderService) GetOrdersByOrderType(orderType string, c *gin.Context) ([]map[string]interface{}, error) {
	// Extract jwt token from header request
    token, err := security.ExtractHeaderToken(c)
    if err != nil {
        return nil, err
    }
	allOrders, err := s.orderRepo.GetOrdersByOrderType(orderType)
	if err != nil {
		return nil, fmt.Errorf("failed to fetch all orders: %w", err)
	}
	
	var updatedOrdersWithSettings []map[string]interface{}
    for _, order := range allOrders {
		updatedOrder := make(map[string]interface{})
		for k, v := range order {
			updatedOrder[k] = v
		}

		// Check order type
		orderType, ok := order["type"]
		if !ok {
			return nil, &NotFoundError{
				Message: "Order type is missing",
				Code:    http.StatusNotFound,
			}
		}

		// If order type is BUY, skip processing
		if orderType == string(models.OrderTypeBuy){
			updatedOrdersWithSettings = append(updatedOrdersWithSettings, updatedOrder)
			continue
		}

		// If order type is SELL, process bank details
		if orderType == string(models.OrderTypeSell){
			// Extract bankId and userId
			bankId, ok := order["bank_id"]
			if !ok || bankId == nil {
				return nil, &NotFoundError{
					Message: "Bank ID is missing",
					Code:    http.StatusNotFound,
				}
			}

			userId, ok := order["user_id"]
			if !ok {
				return nil, &NotFoundError{
					Message: "User ID is missing",
					Code:    http.StatusNotFound,
				}
			}

			userIdValue, ok := userId.(uint)
			if !ok {
				return nil, &NotFoundError{
					Message: "User ID is not a valid uint",
					Code:    http.StatusNotFound,
				}
			}

			bankIdValue, ok := bankId.(*uint)
			if !ok {
				return nil, &NotFoundError{
					Message: "Bank ID is not a valid uint",
					Code:    http.StatusNotFound,
				}
			}

			// Fetch bank details
			bankDetails, err := s.banklistService.GetBankDetails(userIdValue, *bankIdValue, token)
			if err != nil {
				return nil, responses.CreateErrorResponse("Bank details does not match the system.", err.Error())
			}

			// Append bank details to the order
			updatedOrder["bank_details"] = bankDetails
			updatedOrdersWithSettings = append(updatedOrdersWithSettings, updatedOrder)
		}
	}

    return updatedOrdersWithSettings, nil

}

// Fetch User Order List By UserId
func (s *orderService) GetUserOrderList(userID uint, c *gin.Context)  ([]map[string]interface{}, error){
  // Extract jwt token from header request
    token, err := security.ExtractHeaderToken(c)
    if err != nil {
        return nil, err
    }
	// Call the repository method
	allOrders, err := s.orderRepo.GetOrdersByUserID(userID)
	if err != nil {
		return nil, fmt.Errorf("failed to fetch all orders: %w", err)
	}
	
	var updatedOrdersWithSettings []map[string]interface{}
    for _, order := range allOrders {
		updatedOrder := make(map[string]interface{})
		for k, v := range order {
			updatedOrder[k] = v
		}

		// Check order type
		orderType, ok := order["type"]
		if !ok {
			return nil, &NotFoundError{
				Message: "Order type is missing",
				Code:    http.StatusNotFound,
			}
		}

		// If order type is BUY, skip processing
		if orderType == string(models.OrderTypeBuy){
			updatedOrdersWithSettings = append(updatedOrdersWithSettings, updatedOrder)
			continue
		}

		// If order type is SELL, process bank details
		if orderType == string(models.OrderTypeSell){
			// Extract bankId and userId
			bankId, ok := order["bank_id"]
			if !ok || bankId == nil {
				return nil, &NotFoundError{
					Message: "Bank ID is missing",
					Code:    http.StatusNotFound,
				}
			}

			userId, ok := order["user_id"]
			if !ok {
				return nil, &NotFoundError{
					Message: "User ID is missing",
					Code:    http.StatusNotFound,
				}
			}

			userIdValue, ok := userId.(uint)
			if !ok {
				return nil, &NotFoundError{
					Message: "User ID is not a valid uint",
					Code:    http.StatusNotFound,
				}
			}

			bankIdValue, ok := bankId.(*uint)
			if !ok {
				return nil, &NotFoundError{
					Message: "Bank ID is not a valid uint",
					Code:    http.StatusNotFound,
				}
			}

			// Fetch bank details
			bankDetails, err := s.banklistService.GetBankDetails(userIdValue, *bankIdValue, token)
			if err != nil {
				return nil, responses.CreateErrorResponse("Bank details does not match the system.", err.Error())
			}

			// Append bank details to the order
			updatedOrder["bank_details"] = bankDetails
			updatedOrdersWithSettings = append(updatedOrdersWithSettings, updatedOrder)
		}
	}

    return updatedOrdersWithSettings, nil
}
 
// Fetch User Bank Details By UserId
func (s *orderService) GetBankDetailsByUserID(userID uint, bankIDs []uint, jwt string) ([]dto.BankListDTO, error) {
	// Base URL for the external service that provides bank details
	baseURL := os.Getenv("DEPOSIT_AND_WITHDRA_SERVICE_URL")
	if baseURL == "" {
		return nil, fmt.Errorf("DEPOSIT_AND_WITHDRA_SERVICE_URL not set in environment variables")
	}

	// Initialize a slice to hold the bank details
	var bankDetailsList []dto.BankListDTO

	// Create the HTTP client
	client := &http.Client{}

	// Loop over the bankIDs and fetch bank details for each one
	for _, bankID := range bankIDs {
		reqURL := fmt.Sprintf("%s/api/banklist/%d/%d", baseURL, userID, bankID)

		// Create the request with the Authorization header
		req, err := http.NewRequest("GET", reqURL, nil)
		if err != nil {
			return nil, fmt.Errorf("failed to create request for bankID %d: %w", bankID, err)
		}

		// Set the Authorization header
		req.Header.Set("Authorization", fmt.Sprintf("Bearer %s", jwt))

		// Send the GET request
		resp, err := client.Do(req)
		if err != nil {
			return nil, fmt.Errorf("failed to send request for bankID %d: %w", bankID, err)
		}
		defer resp.Body.Close()

		// Check if the response status code is OK (200)
		if resp.StatusCode != http.StatusOK {
			return nil, fmt.Errorf("failed to fetch bank details for bankID %d, status code: %d", bankID, resp.StatusCode)
		}

		// Decode the response body into a BankListDTO
		var bankDetails dto.BankListDTO
		if err := json.NewDecoder(resp.Body).Decode(&bankDetails); err != nil {
			return nil, fmt.Errorf("failed to decode response for bankID %d: %w", bankID, err)
		}

		// Append the bank details to the list
		bankDetailsList = append(bankDetailsList, bankDetails)
	}

	// Return the list of bank details
	return bankDetailsList, nil
}

// FetchAllOrders retrieves all orders with their associated payment settings.
func (s *orderService) FetchAllOrders(c *gin.Context) ([]map[string]interface{}, error) {
	// Extract jwt token from header request
    token, err := security.ExtractHeaderToken(c)
    if err != nil {
        return nil, err
    }
	// Call the repository method
	allOrders, err := s.orderRepo.FetchAllOrders()
	if err != nil {
		return nil, fmt.Errorf("failed to fetch all orders: %w", err)
	}
	
	var updatedOrdersWithSettings []map[string]interface{}
    for _, order := range allOrders {
		updatedOrder := make(map[string]interface{})
		for k, v := range order {
			updatedOrder[k] = v
		}

		// Check order type
		orderType, ok := order["type"]
		if !ok {
			return nil, &NotFoundError{
				Message: "Order type is missing",
				Code:    http.StatusNotFound,
			}
		}

		// If order type is BUY, skip processing
		if orderType == string(models.OrderTypeBuy){
			updatedOrdersWithSettings = append(updatedOrdersWithSettings, updatedOrder)
			continue
		}

		// If order type is SELL, process bank details
		if orderType == string(models.OrderTypeSell){
			// Extract bankId and userId
			bankId, ok := order["bank_id"]
			if !ok || bankId == nil {
				return nil, &NotFoundError{
					Message: "Bank ID is missing",
					Code:    http.StatusNotFound,
				}
			}

			userId, ok := order["user_id"]
			if !ok {
				return nil, &NotFoundError{
					Message: "User ID is missing",
					Code:    http.StatusNotFound,
				}
			}

			userIdValue, ok := userId.(uint)
			if !ok {
				return nil, &NotFoundError{
					Message: "User ID is not a valid uint",
					Code:    http.StatusNotFound,
				}
			}

			bankIdValue, ok := bankId.(*uint)
			if !ok {
				return nil, &NotFoundError{
					Message: "Bank ID is not a valid uint",
					Code:    http.StatusNotFound,
				}
			}

			// Fetch bank details
			bankDetails, err := s.banklistService.GetBankDetails(userIdValue, *bankIdValue, token)
			if err != nil {
				return nil, responses.CreateErrorResponse("Bank details does not match the system.", err.Error())
			}

			// Append bank details to the order
			updatedOrder["bank_details"] = bankDetails
			updatedOrdersWithSettings = append(updatedOrdersWithSettings, updatedOrder)
		}
	}

    return updatedOrdersWithSettings, nil
}
