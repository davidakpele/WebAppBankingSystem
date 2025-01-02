package com.example.deposit.models;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.example.deposit.enums.BannedReasons;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class BlackListedWallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long walletId;

    @Enumerated(EnumType.STRING)
    private BannedReasons bankBannedReasons;

    private Boolean isBlock;

    private BannedReasons actionType;

    private Timestamp timestamp;
    
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
