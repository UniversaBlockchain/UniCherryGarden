package com.myodov.unicherrygarden.ethereum;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.Assert.*;

public class EthUtilsWeiTest {
    @Test
    public void testWeiConstants() {
        assertEquals(
                BigInteger.valueOf(1_000_000_000_000_000_000l),
                EthUtils.Wei.WEI_IN_ETHER
        );
        assertEquals(
                BigInteger.valueOf(1_000_000_000l),
                EthUtils.Wei.GWEI_IN_ETHER
        );
    }

    @Test
    public void testValueFromWeis() {
        assertEquals(
                BigDecimal.ONE,
                EthUtils.Wei.valueFromWeis(BigInteger.valueOf(1_000_000_000_000_000_000l))
        );
        assertEquals(
                BigDecimal.ZERO,
                EthUtils.Wei.valueFromWeis(BigInteger.ZERO)
        );
        assertEquals(
                new BigDecimal("0.000000079"),
                EthUtils.Wei.valueFromWeis(BigInteger.valueOf(79_000_000_000l))
        );
    }

    @Test
    public void testValueToWeis() {
        assertEquals(
                BigInteger.valueOf(1_000_000_000_000_000_000l),
                EthUtils.Wei.valueToWeis(BigDecimal.ONE)
        );
        assertEquals(
                BigInteger.ZERO,
                EthUtils.Wei.valueToWeis(BigDecimal.ZERO)
        );
        assertEquals(
                BigInteger.valueOf(79_000_000_000l),
                EthUtils.Wei.valueToWeis(new BigDecimal("0.000000079"))
        );
    }

    @Test
    public void testValueFromGweis() {
        assertEquals(
                BigDecimal.ONE,
                EthUtils.Wei.valueFromGweis(BigDecimal.valueOf(1_000_000_000l))
        );
        assertEquals(
                BigDecimal.ZERO,
                EthUtils.Wei.valueFromGweis(BigDecimal.ZERO)
        );
        assertEquals(
                new BigDecimal("0.000000079"),
                EthUtils.Wei.valueFromGweis(new BigDecimal(79))
        );
    }

    @Test
    public void testValueToGweis() {
        assertEquals("Trailing zeros may be skipped, so need to use compareTo",
                0,
                BigDecimal.valueOf(1_000_000_000l).compareTo(
                    EthUtils.Wei.valueToGweis(BigDecimal.ONE)
                )
        );
        assertEquals(
                BigDecimal.ZERO,
                EthUtils.Wei.valueToGweis(BigDecimal.ZERO)
        );
        assertEquals(
                new BigDecimal(79),
                EthUtils.Wei.valueToGweis(new BigDecimal("0.000000079"))
        );
    }
}
