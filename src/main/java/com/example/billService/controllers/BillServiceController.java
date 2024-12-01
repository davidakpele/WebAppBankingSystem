package com.example.billService.controllers;

import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.billService.payloads.PayCableRequest;
import com.example.billService.payloads.PayElectricityRequest;
import com.example.billService.payloads.PurchaseAirtimeRequest;
import com.example.billService.responses.ErrorValidation;
import com.example.billService.services.BillService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/bill")
@RequiredArgsConstructor
public class BillServiceController {

    private final BillService billService;

    @PostMapping("/buy/airtime")
    public ResponseEntity<?> buyAirtime(@RequestBody PurchaseAirtimeRequest request, Authentication authentication) {
   
        if (request.getServiceProvider() == null || request.getServiceProvider() == "") {
            return ErrorValidation.createResponse(
                    "Service provider require.*", HttpStatus.BAD_REQUEST,
                    "Please provide your service provider network.");
        }

        if (request.getMobileNumber() == null || request.getMobileNumber() == "") {
            return ErrorValidation.createResponse(
                    "Recipient mobile number require.*", HttpStatus.BAD_REQUEST,
                    "Please provide the line you want to recharge.");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            return ErrorValidation.createResponse(
                    "Amount require.*", HttpStatus.BAD_REQUEST,
                    "Please provide amount you want to recharge/load on this line.");
        } else if (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.valueOf(100)) < 0) {
            return ErrorValidation.createResponse(
                    "Amount cannot be less than N100", HttpStatus.BAD_REQUEST,
                    "Please provide a valid amount");
        }

        return billService.buyAirTime(authentication, request);
    }

    @PostMapping("/buy/databundle")
    public ResponseEntity<?> buyDataBundle(@RequestBody PurchaseAirtimeRequest request, Authentication authentication) {

        if (request.getServiceProvider() == null || request.getServiceProvider() == "") {
            return ErrorValidation.createResponse(
                    "Service provider require.*", HttpStatus.BAD_REQUEST,
                    "Please provide your service provider network.");
        }

        if (request.getProduct() == null || request.getProduct() == "") {
            return ErrorValidation.createResponse(
                    "Please the type of service bundle you want purchase is require.*", HttpStatus.BAD_REQUEST,
                    "Provide the service bundle");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            return ErrorValidation.createResponse(
                    "Amount require.*", HttpStatus.BAD_REQUEST,
                    "Please provide amount you want to recharge/load on this line.");
        }
        return billService.buyDataBundle(authentication, request);
    }

    @PostMapping("/pay/cable")
    public ResponseEntity<?> payUserCable(@RequestBody PayCableRequest request, Authentication authentication) {

        if (request.getCableNumber() == null || request.getCableNumber().equals(0)) {
            return ErrorValidation.createResponse(
                    "Cable Number requires.*", HttpStatus.BAD_REQUEST,
                    "Please provide your cable number");
        }

        if (request.getCableOwnerName() == null || request.getCableOwnerName() == "") {
            return ErrorValidation.createResponse(
                    "Cable owner name requires.*", HttpStatus.BAD_REQUEST,
                    "Please provider the cable owner name for proper confirmation.");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            return ErrorValidation.createResponse(
                    "Amount require.*", HttpStatus.BAD_REQUEST,
                    "Please provide amount you want to recharge/load on this line.");
        }

        return billService.payUserCable(authentication, request);
    }

    @PostMapping("/pay/electricity")
    public ResponseEntity<?> payUserElectricity(@RequestBody PayElectricityRequest request, Authentication authentication) {

        if (request.getMeterNumber().isEmpty() || request.getMeterNumber() == null) {
            return ErrorValidation.createResponse(
                    "The meter number is requires.*", HttpStatus.BAD_REQUEST,
                    "Please provide the meter number.");
        }

        if (request.getServiceProvider().isEmpty() || request.getServiceProvider() == null) {
            return ErrorValidation.createResponse(
                    "The service provider is requires.*", HttpStatus.BAD_REQUEST,
                    "Please provide the service provider.");
        }

        if (request.getMeterType().isEmpty() || request.getMeterType() == null) {
            return ErrorValidation.createResponse(
                    "The meter type is requires.*", HttpStatus.BAD_REQUEST,
                    "Please the meter type provider.");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            return ErrorValidation.createResponse(
                    "Amount require.*", HttpStatus.BAD_REQUEST,
                    "Please provide amount you want to recharge/load.");
        }
        return billService.payUserElectricity(authentication, request);
    }

    @PostMapping("/pay/internet")
    public ResponseEntity<?> payUserInternetSubscription(@RequestBody PurchaseAirtimeRequest request, Authentication authentication) {
        if (request.getServiceProvider() == null || request.getServiceProvider() == "") {
            return ErrorValidation.createResponse(
                    "Service provider require.*", HttpStatus.BAD_REQUEST,
                    "Please provide your service provider network.");
        }
        return null;
    }

    @PostMapping("/load/bet/wallet")
    public ResponseEntity<?> loadUserBettingWallet(@RequestBody PurchaseAirtimeRequest request, Authentication authentication) {
    
        return null;
    }
}
