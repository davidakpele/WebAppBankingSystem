package com.example.deposit.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "wallet")
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long userId;

    @ElementCollection
    @MapKeyColumn(name = "currency_code")
    @Column(name = "balance")
    @CollectionTable(name = "wallet_balances", joinColumns = @JoinColumn(name = "wallet_id"))
    private Map<String, BigDecimal> balance; // Store currency balances as key-value pairs (e.g., USD -> 500.00)

    private String password;

    @CreationTimestamp
    private LocalDateTime createdOn;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @UpdateTimestamp
    private LocalDateTime updatedOn;

}
