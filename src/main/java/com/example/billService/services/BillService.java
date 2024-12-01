package com.example.billService.services;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import com.example.billService.payloads.PayCableRequest;
import com.example.billService.payloads.PayElectricityRequest;
import com.example.billService.payloads.PurchaseAirtimeRequest;

public interface BillService {
    ResponseEntity<?> buyAirTime(Authentication authentication, PurchaseAirtimeRequest request);

    ResponseEntity<?> buyDataBundle(Authentication authentication, PurchaseAirtimeRequest request);

    ResponseEntity<?> payUserCable(Authentication authentication, PayCableRequest request);

    ResponseEntity<?> payUserElectricity(Authentication authentication, PayElectricityRequest request);

    ResponseEntity<?> payUserInternetSubscription(Authentication authentication, PurchaseAirtimeRequest request);

    ResponseEntity<?> loadUserBettingWallet(Authentication authentication, PayElectricityRequest request);
}   
