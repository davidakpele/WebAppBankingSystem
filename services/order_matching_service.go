package services

import (
	"context"
	"fmt"
	"time"
	"wallet-app/models"
	"wallet-app/repository"
	"github.com/shopspring/decimal"
)

type OrderMatchingService struct {
    orderRepo              repository.OrderRepository
    coinRepo               repository.CoinRepository
    userService            UserAPIService
    coinTransactionRepo    repository.HistoryRepository
    coinBookingRepo        repository.AssetsBookingRepository
    paymentSettingRepo     repository.PaymentSettingRepository
}

func NewOrderMatchingService(orderRepo repository.OrderRepository, coinRepo repository.CoinRepository, userService UserAPIService,coinTransactionRepo repository.HistoryRepository, coinBookingRepo repository.AssetsBookingRepository, paymentSettingRepo repository.PaymentSettingRepository,) *OrderMatchingService {
    return &OrderMatchingService{
        orderRepo:           orderRepo,
        coinRepo:            coinRepo,
        userService:         userService,
        coinTransactionRepo: coinTransactionRepo,
        coinBookingRepo:     coinBookingRepo,
        paymentSettingRepo:  paymentSettingRepo,
    }
}

func (s *OrderMatchingService) MatchOrders(ctx context.Context, savedOrder *models.Order, paymentMethod models.PaymentMethodType) error {
    var matchingOrders []models.Order
    var err error

    price := savedOrder.Price.InexactFloat64()

    if savedOrder.Type == models.OrderTypeBuy {
        priceDecimal := decimal.NewFromFloat(price)
        matchingOrders, err = s.orderRepo.FindSellOrdersForMatching(savedOrder.TradingPair, priceDecimal)

    } else {
        matchingOrders, err = s.orderRepo.FindBuyOrdersForMatching(savedOrder.TradingPair, price)
        
    }

    if err != nil {
        return fmt.Errorf("error fetching matching orders: %w", err)
    }

    if len(matchingOrders) == 0 {
        return nil
    }

    return s.processMatchingOrders(savedOrder, matchingOrders, paymentMethod)
}

func (s *OrderMatchingService) processMatchingOrders(newOrder *models.Order, matchingOrders []models.Order, paymentMethod models.PaymentMethodType) error {
    var paymentType models.PaymentMethodType
    for _, matchingOrder := range matchingOrders {
        if newOrder.Type == models.OrderTypeSell {
            paymentType = paymentMethod
        }else{
            // Fetch seller payment method from payment settings repo
			paymentSettings, err := s.paymentSettingRepo.FindByUserIdAndOrderId(matchingOrder.ID, matchingOrder.UserID)
			if err != nil {
				return fmt.Errorf("failed to fetch payment settings: %w", err)
			}
			if paymentSettings == nil {
				return fmt.Errorf("no payment settings found for order ID %d and user ID %d", matchingOrder.ID, matchingOrder.UserID)
			}
			paymentType = paymentSettings.PaymentMethod
        }
        var buyerId, sellerOrderID, sellerId uint
        // Determine buyerId and sellerId based on matchingOrder.Type
        if matchingOrder.Type == models.OrderTypeBuy {
            buyerId = matchingOrder.UserID
            sellerId = newOrder.UserID
            sellerOrderID = newOrder.ID
        } else if matchingOrder.Type == models.OrderTypeSell {
            sellerId = matchingOrder.UserID
            buyerId = newOrder.UserID
            sellerOrderID = matchingOrder.ID
        }   
        // Save Booking
        if err := s.saveCoinBooking(buyerId, sellerId, sellerOrderID); err != nil {
            return fmt.Errorf("failed to save coin booking: %w", err)
        }
        s.updateOrderStatus(newOrder)
        s.updateOrderStatus(&matchingOrder)
        // Save the updated orders
        if err := s.orderRepo.SaveOrder(newOrder); err != nil {
            return fmt.Errorf("failed to save new order: %w", err)
        }
        if err := s.orderRepo.SaveOrder(&matchingOrder); err != nil {
            return fmt.Errorf("failed to save matching order: %w", err)
        }
        
        // Record BUY transaction with fees
        if err := s.saveTransactionHistory(buyerId, newOrder, matchingOrder.Amount, s.calculateFee(newOrder, newOrder.Amount), newOrder.Price, models.TransactionTypeBuy, paymentType); err != nil {
            return fmt.Errorf("failed to save buy transaction: %w", err)
        }

        // Record SELL transaction with fees
        if err := s.saveTransactionHistory(sellerId, &matchingOrder, matchingOrder.Amount, s.calculateFee(newOrder, newOrder.Amount), matchingOrder.Price, models.TransactionTypeSell, paymentType); err != nil {
            return fmt.Errorf("failed to save sell transaction: %w", err)
        }
        // Break if newOrder is fully filled
        if newOrder.Status == models.OrderStatusFilled {
            break
        }
    }
    return nil
}

func (s *OrderMatchingService) updateOrderStatus(order *models.Order) {
    order.Status = models.OrderStatusFilled    
}

func (s *OrderMatchingService) saveTransactionHistory(userID uint, order *models.Order, matchAmount, feeAmount, matchPrice decimal.Decimal, tType models.TransactionType, paymentMethod models.PaymentMethodType) error {
    // Fetch the user by user ID
    user, err := s.userService.FindByUserId(userID)
    if err != nil {
        return fmt.Errorf("failed to get user: %w", err)
    }

    // Create the transaction history record
    transaction := &models.History{
        UserID:    uint(user.ID),
        Type:      tType,
        Quantity:  matchAmount, 
        Price:     matchPrice,                
        CoinID:    order.TradingPair,                                    
        Fee:       feeAmount,  
        PaymentMethod: paymentMethod,             
        CreatedOn: time.Now(),
    }

    // Save the transaction history
    return s.coinTransactionRepo.SaveHistory(transaction)
}

func (s *OrderMatchingService) calculateFee(order *models.Order, matchedAmount decimal.Decimal) decimal.Decimal {
    var feePercentage decimal.Decimal
    if order.Type == models.OrderTypeBuy {
        if order.IsMaker {
            feePercentage = decimal.NewFromFloat(0.001)
        } else {
            feePercentage = decimal.NewFromFloat(0.002)
        }
    } else {
        if order.IsMaker {
            feePercentage = decimal.NewFromFloat(0.0015)
        } else {
            feePercentage = decimal.NewFromFloat(0.0025)
        }
    }

    return matchedAmount.Mul(order.Price).Mul(feePercentage)
}

func (s *OrderMatchingService) saveCoinBooking(buyerId uint, sellerId uint, orderId uint) error {
    
    coinBooking := &models.Bookings{
        OrderID: orderId,
        BuyerID:  buyerId,
        SellerID: sellerId,
    }

    return s.coinBookingRepo.SaveBooking(coinBooking)
}
