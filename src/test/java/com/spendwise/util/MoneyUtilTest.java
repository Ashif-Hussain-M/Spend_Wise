package com.spendwise.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MoneyUtilTest {

    @Test
    void testZero() {
        BigDecimal zero = MoneyUtil.zero();
        assertEquals(new BigDecimal("0.00"), zero);
        assertEquals(2, zero.scale());
    }

    @Test
    void testOfBigDecimal() {
        BigDecimal val = new BigDecimal("123.456");
        BigDecimal result = MoneyUtil.of(val);
        assertEquals(new BigDecimal("123.46"), result);
    }

    @Test
    void testOfDouble() {
        BigDecimal result = MoneyUtil.of(123.454);
        assertEquals(new BigDecimal("123.45"), result);
    }

    @Test
    void testParse() {
        assertEquals(new BigDecimal("123456.78"), MoneyUtil.parse("Rs 1,23,456.78"));
        assertEquals(new BigDecimal("123456.78"), MoneyUtil.parse("₹ 1,23,456.78"));
        assertEquals(new BigDecimal("500.00"), MoneyUtil.parse("500"));
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.parse(""));
    }

    @Test
    void testFormatRupees() {
        assertEquals("Rs 0.00", MoneyUtil.formatRupees(BigDecimal.ZERO));
        assertEquals("Rs 500.00", MoneyUtil.formatRupees(new BigDecimal("500")));
        assertEquals("Rs 1,000.00", MoneyUtil.formatRupees(new BigDecimal("1000")));
        assertEquals("Rs 12,345.67", MoneyUtil.formatRupees(new BigDecimal("12345.67")));
        assertEquals("Rs 1,23,456.00", MoneyUtil.formatRupees(new BigDecimal("123456")));
        assertEquals("Rs 12,34,567.89", MoneyUtil.formatRupees(new BigDecimal("1234567.89")));
        assertEquals("Rs 1,23,45,678.00", MoneyUtil.formatRupees(new BigDecimal("12345678")));
        assertEquals("-Rs 500.00", MoneyUtil.formatRupees(new BigDecimal("-500")));
    }
}
