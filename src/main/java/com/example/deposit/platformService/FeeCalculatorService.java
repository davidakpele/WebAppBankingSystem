package com.example.deposit.platformService;

import java.math.BigDecimal;

public interface FeeCalculatorService {

    BigDecimal calculateFee(BigDecimal transactionAmount);

}
