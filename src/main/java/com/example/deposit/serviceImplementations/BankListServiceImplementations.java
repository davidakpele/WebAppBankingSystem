package com.example.deposit.serviceImplementations;

import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.example.deposit.config.PaystackApiConfig;
import com.example.deposit.dto.PayStackBankList;
import com.example.deposit.models.BankList;
import com.example.deposit.payloads.BankAccountRequest;
import com.example.deposit.repository.BankListRepository;
import com.example.deposit.responses.PaystackResponse;
import com.example.deposit.services.BankListService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;

@Service
public class BankListServiceImplementations implements BankListService{

    private final PaystackApiConfig paystackApiConfig;
    private final WebClient.Builder webClientBuilder;
    private final BankListRepository bankListRepository;
    private final UserServiceClient userServiceClient;

    public BankListServiceImplementations(
        PaystackApiConfig paystackApiConfig,
        WebClient.Builder webClientBuilder, 
        BankListRepository bankListRepository, 
            UserServiceClient userServiceClient) {
        this.paystackApiConfig = paystackApiConfig;
        this.webClientBuilder = webClientBuilder;
        this.bankListRepository = bankListRepository;
        this.userServiceClient = userServiceClient;
    }
    
    @Override
    public Optional<String> verifyBankAccount(String accountNumber, String bankCode) {
        String url = String.format("%s/bank/resolve?account_number=%s&bank_code=%s", paystackApiConfig.getUrl(), accountNumber, bankCode);

        try {
            Mono<String> response = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + paystackApiConfig.getKey())
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(WebClientResponseException.class, ex -> Mono.just(ex.getResponseBodyAsString()));

            return Optional.ofNullable(response.block()); // Blocking call
        } catch (Exception e) {
            return Optional.of("Error: " + e.getMessage());
        }
    }

    @Override
    public Optional<String> addNewBankDetails(BankAccountRequest request, String token, Authentication authentication) {
        String username = authentication.getName();
        var userInfo = userServiceClient.getUserByUsername(username, token);
        
        BankList bank = BankList.builder()
                .accountHolderName(request.getAccount_holder_name())
                .accountNumber(request.getAccountNumber())
                .bankCode(request.getBankCode())
                .bankName(request.getBankName())
                .userId(userInfo.getId())
                .build();
        bankListRepository.save(bank);
        return Optional.empty();
    }
 

    @Override
    public List<BankList> getBankListByUserId(Long id) {
        return bankListRepository.findByUserId(id);
    }

    @Override
    public Optional<BankList> findUserBankByBankId(Long bankId, Long userId) {
        return bankListRepository.findByIdAndUserId(bankId, userId);
    }

    @Override
    public Optional<List<PayStackBankList>> fetchAllBanks() {
        String url = "https://api.paystack.co/bank";

        try {
            Mono<String> response = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + paystackApiConfig.getKey())
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(WebClientResponseException.class, ex -> Mono.just(ex.getResponseBodyAsString()));

            String responseBody = response.block(); // Blocking call
            ObjectMapper objectMapper = new ObjectMapper();
            PaystackResponse<List<PayStackBankList>> paystackResponse = objectMapper.readValue(responseBody,
                    new TypeReference<PaystackResponse<List<PayStackBankList>>>() {
                    });
            if (paystackResponse.isStatus()) {
                return Optional.ofNullable(paystackResponse.getData());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
