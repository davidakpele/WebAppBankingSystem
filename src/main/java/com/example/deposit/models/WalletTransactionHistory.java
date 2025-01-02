package com.example.deposit.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.example.deposit.enums.CurrencyType;
import com.example.deposit.enums.TransactionType;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "wallet_history")
public class WalletTransactionHistory {
    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    private String sessionId;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private String description;

    private String message;
     
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CurrencyType currencyType; 

    private String status;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    private Timestamp timestamp;
    
    @CreationTimestamp
    private LocalDateTime createdOn;

    @UpdateTimestamp
    private LocalDateTime updatedOn;
}
