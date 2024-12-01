package com.example.deposit.services;

import reactor.core.publisher.Mono;

public interface PaystackService {
    Mono<String> initializePayment(String email, int amount);
    
    Mono<String> verifyPayment(String reference);
}
