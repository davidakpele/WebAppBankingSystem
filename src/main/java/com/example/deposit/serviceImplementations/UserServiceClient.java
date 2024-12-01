package com.example.deposit.serviceImplementations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.example.deposit.dto.UserDTO;
import com.example.deposit.exceptions.UserNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;

@Service
public class UserServiceClient {

    private final WebClient webClient;

    @Autowired
    public UserServiceClient(WebClient.Builder webClientBuilder, @Value("${auth-service.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public UserDTO getUserById(Long id, String token) {
        return this.webClient.get()
                .uri("/api/v1/user/personal-details/{id}", id)
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .bodyToMono(UserDTO.class)
                .block(); 
    }

    public UserDTO getUserByUsername(String username, String token) {
        return this.webClient.get()
                .uri("/api/v1/user/by/username/{username}", username)
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorMessage -> {
                                    if (clientResponse.statusCode().is4xxClientError()) {
                                        String details = extractDetailsFromError(errorMessage);
                                        return Mono.error(new UserNotFoundException("User not found", details));
                                    }
                                    return Mono.error(new RuntimeException("Server error"));
                                }))
                .bodyToMono(UserDTO.class)
                .block();
    }

  
    public UserDTO findUserByUsernameInPublicRoute(String username) {
        return this.webClient.get()
                .uri("/api/v1/user/by/public/username/{username}", username)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorMessage -> {
                                    if (clientResponse.statusCode().is4xxClientError()) {
                                        String details = extractDetailsFromError(errorMessage);
                                        return Mono.error(new UserNotFoundException("User not found", details));
                                    }
                                    return Mono.error(new RuntimeException("Server error"));
                                }))
                .bodyToMono(UserDTO.class)
                .block();
    }

    public boolean UpdateUserTranferPinInUserRecord(String token, Long id, boolean updateUserRecordTransferPin) {
        try {
            // Send PUT request to update the user's transfer pin
            this.webClient.put()
                    .uri("/api/v1/user/{id}/updateUserRecord/transferPin/status", id) 
                    .headers(headers -> headers.setBearerAuth(token))
                    .bodyValue(updateUserRecordTransferPin)
                    .retrieve() 
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> {
                                return response.bodyToMono(String.class)
                                        .flatMap(body -> {
                                            throw new RuntimeException("Error occurred: " + body);
                                        });
                            })
                    .toBodilessEntity() 
                    .block(); 
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String extractDetailsFromError(String errorMessage) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(errorMessage);
            return rootNode.path("message").asText();
        } catch (JsonProcessingException e) {
            return "No details available";
        }
    }

    public UserDTO fetchPublicUserById(Long userId) {
        return this.webClient.get()
                .uri("/api/v1/user/by/public/userId/{userId}", userId)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorMessage -> {
                                    if (clientResponse.statusCode().is4xxClientError()) {
                                        String details = extractDetailsFromError(errorMessage);
                                        return Mono.error(new UserNotFoundException("User not found", details));
                                    }
                                    return Mono.error(new RuntimeException("Server error"));
                                }))
                .bodyToMono(UserDTO.class)
                .block();
    }


}
