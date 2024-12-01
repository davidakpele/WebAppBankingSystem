package com.example.deposit.models;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Column(name = "balance", nullable = false)
    private BigDecimal balance; // Total company revenue

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "created_at", nullable = false)
    private LocalDateTime updatedAt;
}
