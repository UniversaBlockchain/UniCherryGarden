package com.myodov.unicherrygarden.connector.impl;

import com.myodov.unicherrygarden.connector.api.Sender;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;

public class FeeSuggestionImplTest {
    @Test
    public void testFeeSuggestionBasic() {
        final Sender.FeeSuggestion sugg1 = new SenderImpl.FeeSuggestionImpl(
                EthUtils.Wei.valueFromWeis(new BigInteger("1000000000")), // 1 Gwei
                EthUtils.Wei.valueFromWeis(new BigInteger("87814301119")) // 87 Gwei
        );

        // We cannot use assertEquals for comparing BigDecimals

        assertTrue(
                "Estimating some ETH transfer",
                new BigDecimal("0.001844100323499").compareTo(
                        sugg1.estimateTotalFee(BigInteger.valueOf(21_000))
                ) == 0
        );

        assertTrue(
                "Estimating some ERC20 transfer with the limit of 80k gas",
                new BigDecimal("0.00702514408952").compareTo(
                        sugg1.estimateTotalFee(BigInteger.valueOf(80_000))
                ) == 0
        );

        assertTrue(
                "Estimating some ERC20 transfer with the limit of 120k gas",
                new BigDecimal("0.01053771613428").compareTo(
                        sugg1.estimateTotalFee(BigInteger.valueOf(120_000))
                ) == 0
        );
    }
}
