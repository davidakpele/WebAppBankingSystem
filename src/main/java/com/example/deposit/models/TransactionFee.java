package com.example.deposit.models;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "transaction_fee")
public class TransactionFee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal makerFeePercentage; // e.g., 0.1% = 0.001

    @Column(nullable = false)
    private BigDecimal takerFeePercentage; // e.g., 0.2% = 0.002
}
