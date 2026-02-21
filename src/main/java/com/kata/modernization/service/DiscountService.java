package com.kata.modernization.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DiscountService {

    private static final BigDecimal DISCOUNT_THRESHOLD = new BigDecimal("500.00");
    private static final BigDecimal DISCOUNT_PERCENTAGE = new BigDecimal("0.10");

    public DiscountResult calculateDiscount(Long clientId, String name, BigDecimal balance) {
        BigDecimal discount = BigDecimal.ZERO;

        if (balance.compareTo(DISCOUNT_THRESHOLD) > 0) {
            discount = balance.multiply(DISCOUNT_PERCENTAGE).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal finalBalance = balance.subtract(discount);

        return new DiscountResult(clientId, name, balance, discount, finalBalance);
    }

    public record DiscountResult(
            Long clientId,
            String name,
            BigDecimal originalBalance,
            BigDecimal discountApplied,
            BigDecimal finalBalance) {
    }
}
