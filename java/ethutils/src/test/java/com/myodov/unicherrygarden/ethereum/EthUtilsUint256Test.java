package com.myodov.unicherrygarden.ethereum;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class EthUtilsUint256Test {
    @Test
    public void testUint256ConvertableToBigDecimals() {
        assertEquals(
                BigDecimal.ONE,
                EthUtils.Uint256.valueFromUint256(BigInteger.valueOf(1_000_000_000_000_000_000L), 18)
        );
        assertEquals(
                "Example for USDT which has decimals=6",
                new BigDecimal(250),
                EthUtils.Uint256.valueFromUint256(BigInteger.valueOf(250_000_000L), 6)
        );
        assertEquals(
                BigDecimal.ZERO,
                EthUtils.Uint256.valueFromUint256(BigInteger.ZERO, 1)
        );
        assertEquals(
                BigDecimal.ZERO,
                EthUtils.Uint256.valueFromUint256(BigInteger.ZERO, 18)
        );
    }

    @Test
    public void testUint256ConvertableFromBigDecimals() {
        assertEquals(
                BigInteger.valueOf(1_000_000_000_000_000_000L),
                EthUtils.Uint256.valueToUint256(BigDecimal.ONE, 18)
        );
        assertEquals(
                "Example for USDT which has decimals=6",
                BigInteger.valueOf(250_000_000L),
                EthUtils.Uint256.valueToUint256(new BigDecimal(250), 6)
        );
        assertEquals(
                BigInteger.ZERO,
                EthUtils.Uint256.valueToUint256(BigDecimal.ZERO, 1)
        );
        assertEquals(
                BigInteger.ZERO,
                EthUtils.Uint256.valueToUint256(BigDecimal.ZERO, 18)
        );
    }
}
