package com.example.deposit.models;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "revenue")
public class Revenue {
    @Id
    private Long id;

    @ElementCollection
    @MapKeyColumn(name = "currency_code")
    @Column(name = "balance")
    @CollectionTable(name = "revenue_balances", joinColumns = @JoinColumn(name = "revenue_id"))
    private Map<String, BigDecimal> balance = new HashMap<>();

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "created_at", nullable = false)
    private LocalDateTime updatedAt;
}
