package com.spendwise.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class MoneyUtil {

    private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;
    private static final int DEFAULT_SCALE = 2;

    private MoneyUtil() {
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal of(BigDecimal amount) {
        if (amount == null) {
            return zero();
        }
        return amount.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal of(double amount) {
        return BigDecimal.valueOf(amount).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount text cannot be empty");
        }
        String clean = text.replace("Rs", "")
                .replace("₹", "")
                .replace(",", "")
                .trim();
        return new BigDecimal(clean).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static String formatRupees(BigDecimal amount) {
        if (amount == null) {
            return "Rs 0.00";
        }
        BigDecimal scaled = amount.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
        boolean isNegative = scaled.signum() < 0;
        BigDecimal abs = scaled.abs();

        String formattedAbs = formatIndianNumber(abs);
        if (isNegative) {
            return "-Rs " + formattedAbs;
        }
        return "Rs " + formattedAbs;
    }

    private static String formatIndianNumber(BigDecimal amount) {
        String stringVal = amount.toPlainString();
        int dotIndex = stringVal.indexOf('.');
        String integerPart = dotIndex >= 0 ? stringVal.substring(0, dotIndex) : stringVal;
        String decimalPart = dotIndex >= 0 ? stringVal.substring(dotIndex + 1) : "00";
        if (decimalPart.length() < 2) {
            decimalPart = decimalPart + "0";
        } else if (decimalPart.length() > 2) {
            decimalPart = decimalPart.substring(0, 2);
        }

        if (integerPart.length() <= 3) {
            return integerPart + "." + decimalPart;
        }

        String lastThree = integerPart.substring(integerPart.length() - 3);
        String remaining = integerPart.substring(0, integerPart.length() - 3);

        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = remaining.length() - 1; i >= 0; i--) {
            sb.append(remaining.charAt(i));
            count++;
            if (count % 2 == 0 && i > 0) {
                sb.append(',');
            }
        }
        sb.reverse();
        sb.append(',').append(lastThree).append('.').append(decimalPart);
        return sb.toString();
    }
}
