package com.example.deposit.enums;

public enum CurrencyType {
    USD, // United States Dollar
    EUR, // Euro
    NGN, // Nigerian Naira
    GBP, // British Pound Sterling
    JPY, // Japanese Yen
    AUD, // Australian Dollar
    CAD, // Canadian Dollar
    CHF, // Swiss Franc
    CNY, // Chinese Yuan
    INR; // Indian Rupee

    public static CurrencyType fromString(String value) {
        for (CurrencyType type : CurrencyType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown currency type: " + value);
    }
}
