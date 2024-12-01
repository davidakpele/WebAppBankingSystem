package com.example.billService.payloads;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseAirtimeRequest {

    private String serviceProvider;
    private String mobileNumber;
    private BigDecimal amount;
    private String product;
}
