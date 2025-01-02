package com.example.deposit.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "beneficiary")
public class BeneficiaryDairy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String bankCode;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String bankName;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String accountHolderName;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;
    
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long userId;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @CreationTimestamp
    private LocalDateTime createdOn;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @UpdateTimestamp
    private LocalDateTime updatedOn;
}
