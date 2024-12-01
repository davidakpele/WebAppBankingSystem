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
public class PayCableRequest {

    private String provider;
    private BigInteger cableNumber;
    private String cableOwnerName;
    private String servicePlan;
    private BigDecimal amount;
}
