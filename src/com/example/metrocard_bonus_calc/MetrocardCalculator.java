package com.example.metrocard_bonus_calc;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Performs MetroCard bonus calculations.
 */
public class MetrocardCalculator {
    private static final BigDecimal CENT = BigDecimal.valueOf(0.01);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    
    private BigDecimal bonusMin;
    private BigDecimal bonusPercentage;
    private BigDecimal increment;
    
    /**
     * Constructor.
     */
    public MetrocardCalculator() {
        bonusMin = BigDecimal.ZERO;
        bonusPercentage = BigDecimal.ZERO;
        increment = CENT;
    }
    
    /**
     * Sets the minimum payment needed for a bonus to be applied.
     * 
     * @param  bonusMin minimum payment in USD
     * @throws IllegalArgumentException if bonusMin is negative
     * @throws NullPointerException if bonusMin is null
     */
    public void setBonusMin(BigDecimal bonusMin) {
        if (bonusMin.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Bonus minimum cannot be negative");
        }
        this.bonusMin = bonusMin;
    }

    /**
     * Sets the payment increment.
     * 
     * @param  increment increment in USD
     * @throws IllegalArgumentException if increment is 0 or indivisible by 0.01
     * @throws NullPointerException if increment is null
     */
    public void setIncrement(BigDecimal increment) {
        if (increment.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Increment cannot be 0");
        }
        if (increment.remainder(CENT).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Increment must be divisible by 0.01");
        }
        this.increment = increment;
    }

    /**
     * Sets the bonus percentage.
     * 
     * @param  bonusPercentage a percentage (not a decimal)
     * @throws IllegalArgumentException if bonusPercentage is negative
     * @throws NullPointerException if bonusPercentage is null
     */
    public void setBonusPercentage(BigDecimal bonusPercentage) {
        if (bonusPercentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Bonus percentage cannot be negative");
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

    /**
     * Returns the bonus percentage.
     * 
     * @return the bonus as a percentage
     */
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
     * @return payment amount in USD
     * @throws IllegalArgumentException if an argument is negative
     * @throws NullPointerException if an argument is null
     */
    public BigDecimal computePayment(BigDecimal fare, BigDecimal currentBalance, BigInteger rides) {
        /* 
         * Remember that the equals method takes scale into account. If the
         * scale doesn't matter, use compareTo instead to check for equality.
         */
        if (fare.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fare cannot be negative");
        }
        if (currentBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Current balance cannot be negative");
        }
        if (rides.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("Number of rides cannot be negative");
        }
        BigDecimal target = fare.multiply(new BigDecimal(rides));
        BigDecimal result = target.subtract(currentBalance);
        if (result.compareTo(bonusMin) < 0) {
            return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
        }
        BigDecimal bonusDecimal = bonusPercentage.divide(HUNDRED);
        result = result.divide(bonusDecimal.add(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
        if (result.compareTo(bonusMin) < 0) {
            return bonusMin;
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
     * @param  payment payment amount in USD
     * @return bonus amount in USD
     * @throws IllegalArgumentException if payment is negative
     * @throws NullPointerException if payment is null
     */
    public BigDecimal computeBonus(BigDecimal payment) {
        if (payment.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Payment cannot be negative");
        }
        if (payment.compareTo(bonusMin) < 0) {
            return BigDecimal.ZERO;
        }
        return bonusPercentage.divide(new BigDecimal(100)).multiply(payment)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
