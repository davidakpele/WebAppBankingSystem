package com.example.deposit.serviceImplementations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.example.deposit.dto.UserDTO;
import com.example.deposit.enums.BannedReasons;
import com.example.deposit.exceptions.UserNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
            .uri("/api/v1/user/by/public/userId/{userId}", id)
            .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorMessage -> {
                                    if (clientResponse.statusCode().is4xxClientError()) {
                                        String details = extractDetailsFromError(errorMessage);
                                        return Mono.error(
                                                new UserNotFoundException("User not found: " + details, errorMessage));
                                    }
                                    return Mono.error(new RuntimeException("Server error: " + errorMessage));
                                }))
                .bodyToMono(String.class)  
                .flatMap(response -> {
                    // Convert the raw response to UserDTO
                    UserDTO userDTO = convertToUserDTO(response);
                    return Mono.just(userDTO);
                })
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found: No data returned", token)))
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

    public UserDTO convertToUserDTO(String response) {
        try {
            // Create an ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule()); 

            // Convert the raw JSON response to a UserDTO object
            return objectMapper.readValue(response, UserDTO.class);
        } catch (Exception e) {
            // Log the error details for better debugging
            System.err.println("Error converting response to UserDTO: " + e.getMessage());
            e.printStackTrace();
            
            // Rethrow the exception with additional context
            throw new RuntimeException("Error converting response to UserDTO", e);
        }
    }

    public void updateUserAccountStatus(Long id, BannedReasons suspiciousActivity, String token) {
        try {
            // Send PUT request to update the user's transfer pin
            this.webClient.put()
                    .uri("/api/v1/user/{id}/account/lock", id)
                    .headers(headers -> headers.setBearerAuth(token))
                    .bodyValue(suspiciousActivity)
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
