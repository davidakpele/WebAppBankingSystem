package com.example.deposit.serviceImplementations;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.example.deposit.dto.CoinWalletDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CoinWalletServiceClient {
    private final WebClient webClient;

    public CoinWalletServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8014").build();
    }

    public List<CoinWalletDTO> getCoinWalletsByUserId(Long userId, String token) {
        return webClient.get()
                .uri("/users/{userId}/balances", userId)
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> {
                            // Handle error response (4xx or 5xx)
                            return response.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        System.out.println("Error response body: " + body); 
                                        throw new RuntimeException("Error occurred: " + body);
                                    });
                        })
                .bodyToMono(Map.class) 
                .map(response -> {
                    // Extract the coin_wallets list and map it to CoinWalletDTO
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> coinWalletList = (List<Map<String, Object>>) response.get("coin_wallets");
                    return coinWalletList.stream()
                            .map(wallet -> new CoinWalletDTO(
                                    ((Number) wallet.get("ID")).longValue(),
                                    new BigDecimal(wallet.get("balance").toString()),
                                    (String) wallet.get("crypto_id"),
                                    ((Number) wallet.get("user_id")).longValue(),
                                    (String) wallet.get("wallet_address")
                            ))
                            .collect(Collectors.toList());
                }).block();
    }


}
