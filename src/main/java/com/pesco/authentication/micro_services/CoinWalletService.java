package com.pesco.authentication.micro_services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.pesco.authentication.models.Users;
import com.pesco.authentication.payloads.WalletRequest;

@Service
public class CoinWalletService {

    @Autowired
    private WebClient.Builder webClientBuilder;

    private static final String CREATE_WALLET_URL = "http://localhost:8014/coin/account/create";

    public void createUserWallet(Users user) {
        WebClient webClient = webClientBuilder.build();
        WalletRequest walletRequest = new WalletRequest(user.getId());

        try {
            // Send the request and retrieve the response
            webClient.post()
                    .uri(CREATE_WALLET_URL)
                    .bodyValue(walletRequest)
                    .retrieve()
                    .onStatus(
                            // Handle specific HTTP status codes
                            status -> status.isError(),
                            clientResponse -> clientResponse.bodyToMono(String.class).map(
                                    body -> new RuntimeException("Error response: " + body)))
                    .bodyToMono(String.class) // Parse the response body as a String
                    .block(); // Blocking call for simplicity

        
        } catch (Exception e) {
            // Handle exceptions
            System.err.println("Error creating wallet: " + e.getMessage());
            throw new RuntimeException("Failed to create wallet for user", e);
        }
    }
}
