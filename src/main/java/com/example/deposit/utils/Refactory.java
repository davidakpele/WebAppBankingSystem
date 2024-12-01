package com.example.deposit.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import com.example.deposit.enums.TransactionType;

public class Refactory {
    
    public static String formatBigDecimal(BigDecimal value) {
        String pattern = "#,##0.00";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat(pattern, symbols);
        return decimalFormat.format(value);
    }

    public static String formatEnumValue(TransactionType transactionType) {
        // Convert the enum name to a string and replace underscores with spaces
        return transactionType.name().replace("_", " ");
    }
}
