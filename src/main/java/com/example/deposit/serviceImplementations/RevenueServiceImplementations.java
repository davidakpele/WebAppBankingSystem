package com.example.deposit.serviceImplementations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.deposit.dto.UserDTO;
import com.example.deposit.services.RevenueService;

@Service
public class RevenueServiceImplementations implements RevenueService{
    private final WebClient webClient;

    @Autowired
    public RevenueServiceImplementations(WebClient.Builder webClientBuilder, @Value("${revenue-service.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public UserDTO getUserById(Long id) {
        return this.webClient.get()
                .uri("/personal-details/{id}", id)
                .retrieve()
                .bodyToMono(UserDTO.class)
                .block(); 
    }
}
