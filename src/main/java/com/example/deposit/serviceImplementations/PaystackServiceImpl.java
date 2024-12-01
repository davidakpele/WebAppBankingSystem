package com.example.deposit.serviceImplementations;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.example.deposit.config.PaystackApiConfig;
import com.example.deposit.payloads.PaymentRequest;
import reactor.core.publisher.Mono;

@Service
public class PaystackServiceImpl {

    private final PaystackApiConfig paystackApiConfig;
    private final WebClient.Builder webClientBuilder;

    public PaystackServiceImpl(PaystackApiConfig paystackApiConfig, WebClient.Builder webClientBuilder) {
        this.paystackApiConfig = paystackApiConfig;
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<String> initializePayment(String email, int amount) {
        String url = paystackApiConfig.getUrl() + "/transaction/initialize";
        return webClientBuilder.build()
                .post()
                .uri(url)
                .header("Authorization", "Bearer " + paystackApiConfig.getKey())
                .bodyValue(new PaymentRequest(email, amount))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(WebClientResponseException.class, ex -> Mono.just(ex.getResponseBodyAsString()));
    }

    public Mono<String> verifyPayment(String reference) {
        String url = paystackApiConfig.getUrl() + "/transaction/verify/" + reference;
        return webClientBuilder.build()
                .get()
                .uri(url)
                .header("Authorization", "Bearer " + paystackApiConfig.getKey())
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(WebClientResponseException.class, ex -> Mono.just(ex.getResponseBodyAsString()));
    }
}
