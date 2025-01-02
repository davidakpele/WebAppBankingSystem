package com.example.deposit.platformService;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class FeeCalculatorServiceImplementations implements FeeCalculatorService {
    
    private static final BigDecimal[] TIER_MIN_AMOUNTS = {
        new BigDecimal("100"), 
        new BigDecimal("15000"), 
        new BigDecimal("31000"), 
        new BigDecimal("51000"), 
        new BigDecimal("100000") 
    };

    private static final BigDecimal[] TIER_MAX_AMOUNTS = {
        new BigDecimal("10000"), 
        new BigDecimal("30000"), 
        new BigDecimal("50000"), 
        new BigDecimal("100000"), 
        BigDecimal.valueOf(Double.MAX_VALUE) 
    };

    private static final BigDecimal[] TIER_FEE_PERCENTAGES = {
        new BigDecimal("0.50"), 
        new BigDecimal("0.15"), 
        new BigDecimal("0.35"), 
        new BigDecimal("0.55"), 
        new BigDecimal("1.00") 
    };

    private static final BigDecimal[] VARIABLE_FEE_TIER_MIN_AMOUNTS = {
        new BigDecimal("100000"), 
        new BigDecimal("500000"), 
        new BigDecimal("1000000")
    };

    private static final BigDecimal[] VARIABLE_FEE_TIER_ADDITIONAL_PERCENTAGES = {
        new BigDecimal("0.50"), 
        new BigDecimal("0.75"), 
        new BigDecimal("1.00")
    };

    public BigDecimal calculateFee(BigDecimal transactionAmount) {
        for (int i = 0; i < TIER_MIN_AMOUNTS.length; i++) {
            if (transactionAmount.compareTo(TIER_MIN_AMOUNTS[i]) >= 0 
                && transactionAmount.compareTo(TIER_MAX_AMOUNTS[i]) <= 0) {
                BigDecimal feePercentage = TIER_FEE_PERCENTAGES[i];
                if (transactionAmount.compareTo(new BigDecimal("100000")) > 0) {
                    feePercentage = calculateVariableFeePercentage(transactionAmount);
                }
                return transactionAmount.multiply(feePercentage).divide(BigDecimal.valueOf(100));
            } else if (transactionAmount.compareTo(TIER_MAX_AMOUNTS[i]) > 0) {
                // Calculate fee up to the maximum amount of the tier
                BigDecimal fee = TIER_MAX_AMOUNTS[i].multiply(TIER_FEE_PERCENTAGES[i]).divide(BigDecimal.valueOf(100));
                // Recursively calculate the remaining fee
                return fee.add(calculateFee(transactionAmount.subtract(TIER_MAX_AMOUNTS[i])));
            }
        }
        // Default to a minimum fee if transaction amount is below the lowest tier
        return new BigDecimal("5");
    }

    private BigDecimal calculateVariableFeePercentage(BigDecimal transactionAmount) {
        BigDecimal baseFeePercentage = new BigDecimal("1.00");
        for (int i = 0; i < VARIABLE_FEE_TIER_MIN_AMOUNTS.length; i++) {
            if (transactionAmount.compareTo(VARIABLE_FEE_TIER_MIN_AMOUNTS[i]) >= 0) {
                BigDecimal additionalFeePercentage = VARIABLE_FEE_TIER_ADDITIONAL_PERCENTAGES[i];
                return baseFeePercentage.add(additionalFeePercentage);
            }
        }
        return baseFeePercentage;
    }
}
