package com.example.metrocard_bonus_calc;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/** Performs MetroCard bonus calculations. */
public class MetrocardCalculator {
    private static final BigDecimal CENT    = BigDecimal.valueOf(0.01);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    
    private BigDecimal bonusMin;
    private BigDecimal bonusPercentage;
    private BigDecimal increment;
    
    /**
     * Constructs a MetroCard bonus calculator.
     * 
     * @param bonusMin the minimum payment amount in USD required for a bonus
     * @param bonusPercentage the bonus percentage
     * @param increment the payment increment in USD
     */
    public MetrocardCalculator(BigDecimal bonusMin, BigDecimal bonusPercentage,
            BigDecimal increment) {
        
        setBonusMin(bonusMin);
        setBonusPercentage(bonusPercentage);
        setIncrement(increment);
    }
    
    /**
     * Sets the minimum payment needed for a bonus to be applied.
     * 
     * @param  bonusMin the payment amount in USD
     * @throws IllegalArgumentException if bonusMin is negative
     * @throws NullPointerException if bonusMin is null
     */
    public void setBonusMin(BigDecimal bonusMin) {
        if (bonusMin.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Bonus minimum must not be negative");
        }
        this.bonusMin = bonusMin;
    }

    /**
     * Sets the payment increment.
     * 
     * @param  increment the payment increment in USD
     * @throws IllegalArgumentException if increment is not positive or if it
               is not a multiple of 0.01
     * @throws NullPointerException if increment is null
     */
    public void setIncrement(BigDecimal increment) {
        if (increment.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Increment must be positive");
        }
        if (increment.remainder(CENT).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Increment must be a multiple of 0.01");
        }
        this.increment = increment;
    }

    /**
     * Sets the bonus percentage.
     * 
     * @param  bonusPercentage the bonus percentage
     * @throws IllegalArgumentException if bonusPercentage is negative
     * @throws NullPointerException if bonusPercentage is null
     */
    public void setBonusPercentage(BigDecimal bonusPercentage) {
        if (bonusPercentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Bonus percentage must not be negative");
        }
        this.bonusPercentage = bonusPercentage;
    }

    /**
     * Returns the minimum payment needed for a bonus to applied.
     * 
     * @return minimum payment in USD
     */
    public BigDecimal getBonusMin() {
        return bonusMin;
    }

    /**
     * Returns the payment increment.
     * 
     * @return increment in USD
     */
    public BigDecimal getIncrement() {
        return increment;
    }

    /** Returns the bonus percentage. */
    public BigDecimal getBonusPercentage() {
        return bonusPercentage;
    }

    /**
     * Computes the amount which must be added to a card to obtain a given
     * number of rides.
     * 
     * @param  fare the cost of a fare in USD
     * @param  currentBalance the current balance in USD
     * @param  rides the desired number of rides
     * @return payment the payment amount in USD
     * @throws IllegalArgumentException if an argument is negative
     * @throws NullPointerException if an argument is null
     */
    public BigDecimal computePayment(BigDecimal fare, BigDecimal currentBalance,
            BigInteger rides) {
        
        /* 
         * Remember that the equals method takes scale into account. If the
         * scale doesn't matter, use compareTo instead to check for equality.
         */
        if (fare.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fare must not be negative");
        }
        if (currentBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Current balance must not be negative");
        }
        if (rides.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("Number of rides must not be negative");
        }
        BigDecimal target = fare.multiply(new BigDecimal(rides));
        BigDecimal result = target.subtract(currentBalance);
        if (result.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (result.compareTo(bonusMin) >= 0) {
            BigDecimal bonusDecimal = bonusPercentage.divide(HUNDRED);
            result = result.divide(bonusDecimal.add(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
            if (result.compareTo(bonusMin) <= 0) {
                return bonusMin.max(increment);
            }
        }
        /* The result is adjusted to be divisible by the payment increment. */
        BigDecimal remainder = result.remainder(increment);
        if (remainder.compareTo(BigDecimal.ZERO) != 0) {
            result = result.add(increment.subtract(remainder));
        }
        return result;
    }
    
    /**
     * Computes the bonus earned on a given payment.
     * 
     * @param  payment the payment amount in USD
     * @return bonus the bonus amount in USD
     * @throws IllegalArgumentException if payment is negative
     * @throws NullPointerException if payment is null
     */
    public BigDecimal computeBonus(BigDecimal payment) {
        if (payment.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Payment must not be negative");
        }
        if (payment.compareTo(bonusMin) < 0) {
            return BigDecimal.ZERO;
        }
        return bonusPercentage.divide(new BigDecimal(100)).multiply(payment)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
