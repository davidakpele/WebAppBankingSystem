package services

import (
	"errors"
	"wallet-app/models"
	"wallet-app/repository"
	"github.com/shopspring/decimal"
	"gorm.io/gorm"
	"time"
)

type EscrowService interface {
	CreateEscrow(order *models.Order, matchedAmount decimal.Decimal) error
	UpdateEscrowStatus(escrowID uint, status models.EscrowStatus) error
	GetPendingVerifications() ([]models.Escrow, error)
	ReleaseEscrowToBuyer(escrow *models.Escrow) error
}

type escrowService struct {
	escrowRepo      repository.EscrowRepository
	orderRepo       repository.OrderRepository
	coinWalletSvc   WalletService
	coinBookingRepo repository.AssetsBookingRepository
}

func NewEscrowService(
	escrowRepo repository.EscrowRepository,
	orderRepo repository.OrderRepository,
	coinWalletSvc WalletService,
	coinBookingRepo repository.AssetsBookingRepository,
) EscrowService {
	return &escrowService{
		escrowRepo:      escrowRepo,
		orderRepo:       orderRepo,
		coinWalletSvc:   coinWalletSvc,
		coinBookingRepo: coinBookingRepo,
	}
}

func (s *escrowService) CreateEscrow(order *models.Order, matchedAmount decimal.Decimal) error {
	escrow := &models.Escrow{
		OrderID:   order.ID,
		Amount:    matchedAmount,
		Status:    models.EscrowStatusOpen,
		CreatedAt: time.Now(),
	}

	return s.escrowRepo.Save(escrow)
}

func (s *escrowService) UpdateEscrowStatus(escrowID uint, status models.EscrowStatus) error {
	escrow, err := s.escrowRepo.FindByID(escrowID)
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return errors.New("escrow not found")
		}
		return err
	}

	escrow.Status = status
	return s.escrowRepo.Update(escrow)
}

func (s *escrowService) GetPendingVerifications() ([]models.Escrow, error) {
	return s.escrowRepo.FindAllByStatus(models.EscrowStatusPending)
}

func (s *escrowService) ReleaseEscrowToBuyer(escrow *models.Escrow) error {
	sellOrder, err := s.orderRepo.FindByID(escrow.OrderID)
	if err != nil {
		return errors.New("sell order not found")
	}

	if sellOrder.Type != models.OrderTypeSell {
		return nil
	}

	coinBooking, err := s.coinBookingRepo.FindByOrderID(sellOrder.ID)
	if err != nil && !errors.Is(err, gorm.ErrRecordNotFound) {
		return errors.New("failed to retrieve coin booking")
	}

	buyerID := uint(0)
	asset := sellOrder.TradingPair
	amountToTransfer := escrow.Amount

	if coinBooking != nil {
		buyerID = coinBooking.BuyerID
		s.coinBookingRepo.DeleteByID(coinBooking.ID)
	} else {
		priceFloat64, _ := sellOrder.Price.Float64()
		amountFloat64, _ := sellOrder.Amount.Float64()

		buyOrder, err := s.orderRepo.FindMatchingOrder(
			sellOrder.TradingPair,
			priceFloat64,
			amountFloat64,
			models.OrderTypeBuy,
		)
		if err != nil {
			return errors.New("buy order not found")
		}
		buyerID = buyOrder.UserID
	}

	// Update Seller FillAmount
	if err := s.coinWalletSvc.UpdateFillAmount(sellOrder.UserID, sellOrder.TradingPair, amountToTransfer); err != nil {
		return errors.New("failed to update fill amount from escrow service")
	}

	return s.coinWalletSvc.CreditToBuyer(buyerID, amountToTransfer, asset)
}
