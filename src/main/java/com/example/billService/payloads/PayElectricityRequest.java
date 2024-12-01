package com.example.billService.payloads;

import java.math.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PayElectricityRequest {
    private String meterNumber;
    private String serviceProvider;
    private String meterType;
    private BigDecimal amount;
}
