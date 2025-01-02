package controllers

import (
	"encoding/base64"
	"fmt"
	"net/http"
	"os"
	"strconv"
	"strings"
	"wallet-app/dto"
	"wallet-app/models"
	"wallet-app/payloads"
	"wallet-app/responses"
	"wallet-app/services"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v4"
	"github.com/joho/godotenv"
)

// OrderController struct
type OrderController struct {
	orderService services.OrderService
	userService  services.UserAPIService
}

type LocalBankDetailsDTO struct {
    Order       models.Order    `json:"order"`
    BankDetails []dto.BankListDTO `json:"bank_details"`
}

// NewOrderController creates a new OrderController
func NewOrderController(orderService services.OrderService, userService services.UserAPIService) *OrderController {
	return &OrderController{
		orderService: orderService,
		userService:  userService,
	}
}

// CreateOrder handles the POST request for creating a new order
func (ctrl *OrderController) CreateOrder(c *gin.Context) {
	var request payloads.OrderRequest
	// Bind the JSON body to OrderRequest
	if err := c.ShouldBindJSON(&request); err != nil {
		c.JSON(http.StatusBadRequest, responses.ErrorResponse{
			Message: "Invalid input data",
			Details: err.Error(),
		})
		return
	}

	// Check if bank details ID is provided
	if request.BankID == 0 {
		c.JSON(http.StatusBadRequest, responses.ErrorResponse{
			Message: "Please provide bank details Id",
			Details: "Your bank details Id is required.",
		})
		return
	}

	// Create the order
	order, err := ctrl.orderService.CreateOrder(uint(request.UserID), request, c.Request)
	if err != nil {
		c.JSON(http.StatusInternalServerError, responses.ErrorResponse{
			Message: "Order creation failed",
			Details: err.Error(),
		})
		return
	}

	// return the created order in the response
	c.JSON(http.StatusOK, responses.SuccessResponse{
		Message: "Order created successfully",
		Data:    order, 
	})
}

// GetOrderByID handles the GET request to fetch an order by its ID
func (ctrl *OrderController) GetOrderByID(c *gin.Context) {
	// Get the order ID from the URL path parameter
	orderID := c.Param("id")

	// Convert the ID from string to uint (assuming the ID is of type uint)
	id, err := strconv.ParseUint(orderID, 10, 64)
	if err != nil {
		// If there's an error in converting the ID, return a bad request response
		c.JSON(http.StatusBadRequest, responses.ErrorResponse{
			Message: "Invalid order ID",
			Details: "The provided order ID is invalid.",
		})
		return
	}

	// Call the service to fetch the order by its ID
	order, err := ctrl.orderService.FindById(uint(id))
	if err != nil {
		// If the order is not found or an error occurs, return an error response
		c.JSON(http.StatusNotFound, responses.ErrorResponse{
			Message: "Order not found",
			Details: "No order found with the provided ID.",
		})
		return
	}

	// Return the order if found
	c.JSON(http.StatusOK, responses.SuccessResponse{
		Message: "Order retrieved successfully",
		Data:    order,
	})
}

// GetOrders handles the HTTP request to get all orders
func (ctrl *OrderController) HandleFetchAllOrders(c *gin.Context) {
	orders, err := ctrl.orderService.FetchAllOrders(c)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	// Respond with the data
	c.JSON(http.StatusOK, orders)
}

// BuyAssetOnP2PMarket handles buying an asset on the P2P market
func (ctrl *OrderController) BuyAssetOnP2PMarket(c *gin.Context) {
	// Extract orderId from the URL path parameter
	orderIDStr := c.Param("orderId")
	orderID, err := strconv.Atoi(orderIDStr)
	if err != nil || orderID <= 0 {
		c.JSON(http.StatusBadRequest, responses.ErrorResponse{
			Message: "Please provide a valid Order ID",
			Details: "",
		})
		return
	}

	// Extract username from the context (assuming middleware sets it)
	username, exists := c.Get("username")
	if !exists || username == "" {
		c.JSON(http.StatusForbidden, responses.ErrorResponse{
			Message: "User is not authenticated",
			Details: "",
		})
		return
	}

	// Check if user exists
	user, err := ctrl.userService.FindByUsername(username.(string))
	if err != nil || user == nil {
		c.JSON(http.StatusNotFound, responses.ErrorResponse{
			Message: "User does not exist in our system.",
			Details: "",
		})
		return
	}

	// Call the service method with 'true' for buyer action
	err = ctrl.orderService.HandleP2POrderAction(uint(orderID), uint(user.ID))
	if err != nil {
		c.JSON(http.StatusInternalServerError, responses.ErrorResponse{
			Message: "Something went wrong",
			Details: err.Error(),
		})
		return
	}

	// Return success response
	c.JSON(http.StatusOK, responses.SuccessResponse{
		Message: "Order processed successfully.",
		Data:    nil,
	})
}

// GetOrdersWithBankDetailsByOrderType retrieves orders with bank details by order type
func (ctrl *OrderController) GetOrdersWithBankDetailsByOrderType(c *gin.Context) {

	// Extract order type from the URL path parameter
	orderType := c.Param("orderType")
	if strings.TrimSpace(orderType) == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"message": "Order type is required",
		})
		return
	}

	// Fetch orders from the order service
	orders, err := ctrl.orderService.GetOrdersByOrderType(orderType, c)
	if err != nil || len(orders) == 0 {
		c.JSON(http.StatusNotFound, gin.H{
			"message": "No orders found",
		})
		return
	}
	
	// Respond with the data
	c.JSON(http.StatusOK, orders)
		
}

// UpdateOrder handles the PUT request to update an order
func (ctrl *OrderController) UpdateOrder(c *gin.Context) {
	// Extract the order ID from the URL path parameter
	orderID := c.Param("id")
	if orderID == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"message": "Please provide a valid Order ID.",
		})
		return
	}

	// Parse the order ID into an integer
	id, err := strconv.Atoi(orderID)
	if err != nil || id == 0 {
		c.JSON(http.StatusBadRequest, gin.H{
			"message": "Invalid Order ID.",
		})
		return
	}

	// Bind the request body to an Order struct
	var order models.Order
	if err := c.ShouldBindJSON(&order); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"message": "Invalid request body",
			"details": err.Error(),
		})
		return
	}

	// Call the service method to update the order
	updatedOrder, err := ctrl.orderService.UpdateOrder(uint(id), order)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"message": "Failed to update order",
			"details": err.Error(),
		})
		return
	}

	// Return the updated order in the response
	c.JSON(http.StatusOK, gin.H{
		"message": "Order updated successfully",
		"data":    updatedOrder,
	})
}

// CancelOrder handles the DELETE request to cancel an order
func (ctrl *OrderController) CancelOrder(c *gin.Context) {
	// Extract the order ID from the URL path parameter
	orderID := c.Param("id")
	if orderID == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"message": "Order ID is required",
		})
		return
	}

	// Parse the order ID into an integer
	id, err := strconv.Atoi(orderID)
	if err != nil || id == 0 {
		c.JSON(http.StatusBadRequest, gin.H{
			"message": "Invalid Order ID",
		})
		return
	}

	// Call the service method to cancel the order
	err = ctrl.orderService.CancelOrder(uint(id))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"message": "Failed to cancel order",
			"details": err.Error(),
		})
		return
	}

	// Return no content response if the order is successfully canceled
	c.Status(http.StatusNoContent)
}

// DeleteOrderByOrderId handles the DELETE request to delete an order by its ID
func (ctrl *OrderController) DeleteOrderByOrderId(c *gin.Context) {
	// Load environment variables
    if err := godotenv.Load(); err != nil {
        c.JSON(http.StatusForbidden, gin.H{"error": "Error loading .env file"})
        return
    }

    // Get the JWT secret key from environment variables
    jwtSecretKey := os.Getenv("JWT_SECRET_KEY")
    if jwtSecretKey == "" {
        c.JSON(http.StatusInternalServerError, gin.H{"error": "JWT secret key is not set in .env file"})
        return
    }

    // Get the Authorization header
    authHeader := c.GetHeader("Authorization")
    if authHeader == "" {
        c.JSON(http.StatusForbidden, gin.H{"error": "Authorization header is missing"})
        return
    }

    // Extract the token
    tokenString := strings.TrimPrefix(authHeader, "Bearer ")
    if tokenString == authHeader {
        c.JSON(http.StatusForbidden, gin.H{"error": "Authorization token format is incorrect"})
        return
    }

    // Decode the JWT secret key
    decodedKey, err := base64.StdEncoding.DecodeString(jwtSecretKey)
    if err != nil {
        c.JSON(http.StatusForbidden, gin.H{"error": "Failed to decode the secret key"})
        return
    }

	token, nil := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
        if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
            return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
        }
        return decodedKey, nil
    })

    if claims, ok := token.Claims.(jwt.MapClaims); ok && token.Valid {
        // Extract the username from the token claims
        tokenUsername, ok := claims["sub"].(string)
        if !ok || tokenUsername == "" {
            c.JSON(http.StatusForbidden, gin.H{"error": "Username not found in token claims or empty"})
            return
        }

       // Extract the order ID from the URL path parameter
		orderID := c.Param("id")
		if orderID == "" {
			c.JSON(http.StatusBadRequest, gin.H{
				"message": "Order ID is required",
			})
			return
		}

		// Parse the order ID into an integer
		id, err := strconv.Atoi(orderID)
		if err != nil || id == 0 {
			c.JSON(http.StatusBadRequest, gin.H{
				"message": "Invalid Order ID",
			})
			return
		}

		// Fetch user details
		userDTO, err := ctrl.userService.FindByUsername(tokenUsername)
		if err != nil || userDTO.Username != tokenUsername {
			c.JSON(http.StatusForbidden, gin.H{"error": "Unauthorized access"})
			return
		}

		// Call the service method to delete the order
		err = ctrl.orderService.DeleteOrderByOrderID(uint(id), uint(userDTO.ID))
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{
				"message": "Failed to delete order",
				"details": err.Error(),
			})
			return
		}

        // Respond with the address key
        c.Status(http.StatusNoContent)
    } else {
        c.JSON(http.StatusForbidden, gin.H{"error": "Invalid token"})
    }
}

// GetAllUserOrderList handles the GET request to retrieve all orders for a specific user
func (ctrl *OrderController) GetAllUserOrderList(c *gin.Context) {
	
	// Extract the userID from the URL path parameter
	userID := c.Param("userId")
	if userID == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"message": "User ID is required",
		})
		return
	}

	// Parse the userID into an integer
	id, err := strconv.Atoi(userID)
	if err != nil || id == 0 {
		c.JSON(http.StatusBadRequest, gin.H{
			"message": "Invalid User ID",
		})
		return
	}

	user_id, _:= strconv.ParseUint(userID, 10, 64)
	// Fetch the user by username to verify authenticity
	user, err := ctrl.userService.FindByUserId(uint(user_id))
	if err != nil || user == nil {
		c.JSON(http.StatusUnauthorized, gin.H{
			"message": "User does not exist in our system",
		})
		return
	}

	// Check if the user ID matches the authenticated user ID
	if uint(user_id) != uint(user.ID) {
		c.JSON(http.StatusUnauthorized, gin.H{
			"message": "Unauthorized access",
			"details":"User not found in our system.",
		})
		return
	}

	// Fetch the user's orders from the order service
	userOrders, err := ctrl.orderService.GetUserOrderList(uint(id), c)
	if err != nil || len(userOrders) == 0 {
		c.JSON(http.StatusNotFound, gin.H{
			"message": "No orders found for this user",
		})
		return
	}

	// Return the list of user orders
	c.JSON(http.StatusOK, userOrders)
}

// UpdateOrderStatusForTradeCompletion handles the PUT request to update the status of an order for trade completion
func (ctrl *OrderController) UpdateOrderStatusForTradeCompletion(c *gin.Context) {
	// Extract the orderID from the URL path parameter
	orderID := c.Param("orderId")
	if orderID == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"message": "Order ID is required",
		})
		return
	}

	// Parse the orderID into an integer
	id, err := strconv.Atoi(orderID)
	if err != nil || id == 0 {
		c.JSON(http.StatusBadRequest, gin.H{
			"message": "Invalid Order ID",
		})
		return
	}

	// Get the username from the authentication context
	username, exists := c.Get("username")
	if !exists || username == "" {
		c.JSON(http.StatusUnauthorized, gin.H{
			"message": "User not found",
		})
		return
	}

	// Fetch the user by username to verify authenticity
	user, err := ctrl.userService.FindByUsername(username.(string))
	if err != nil || user == nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"message": "User does not exist in our system",
		})
		return
	}

	// Update the order status for trade completion
	updatedOrder, err := ctrl.orderService.UpdateOrderStatusForTradeCompletion(uint(id), uint(user.ID))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"message": err.Error(),
		})
		return
	}

	// Return the updated order
	c.JSON(http.StatusOK, updatedOrder)
}

// HandleGetAllBuyOrdersWithPaymentSettings handles the HTTP request to retrieve all BUY orders with payment settings.
func (h *OrderController) FetchByOrderId(c *gin.Context) {
	// Get the order ID from the URL path parameter
	orderID := c.Param("id")

	// Convert the ID from string to uint (assuming the ID is of type uint)
	id, err := strconv.ParseUint(orderID, 10, 64)
	if err != nil {
		// If there's an error in converting the ID, return a bad request response
		c.JSON(http.StatusBadRequest, responses.ErrorResponse{
			Message: "Invalid order ID",
			Details: "The provided order ID is invalid.",
		})
		return
	}
	// Call service method
	ordersWithSettings, err := h.orderService.FetchOrderByOrderId(uint(id), c)
	if err != nil {
		// Respond with an error
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	// Respond with the retrieved data
	c.JSON(http.StatusOK, ordersWithSettings)
}
