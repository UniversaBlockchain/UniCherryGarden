package com.myodov.unicherrygarden.connector.impl;

import com.myodov.unicherrygarden.api.types.SystemStatus;
import com.myodov.unicherrygarden.connector.api.Sender;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;

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

    @Test
    public void testFeeSuggestionFromBlockchainSystemStatus() {
        // Example 1:
        // {
        //     "data": {
        //         "maxPriorityFeePerGas": "0x3b9aca00",
        //         "block": {
        //             "baseFeePerGas": "0x98f4c8ba5",
        //             "nextBaseFeePerGas": "0x8b902e067"
        //         }
        //     }
        // }

        final Sender.FeeSuggestion sugg = SenderImpl.FeeSuggestionImpl.fromBlockchainSystemStatus(
                SystemStatus.Blockchain.create(
                        SystemStatus.Blockchain.SyncingData.create(15631007, 14631007),
                        SystemStatus.Blockchain.LatestBlock.create(
                                12205550,
                                12481486L,
                                12449124L,
                                BigInteger.valueOf(0x98f4c8ba5L), // 41 Gwei
                                BigInteger.valueOf(0x8b902e067L), // 37 Gwei
                                Instant.ofEpochSecond(0x60704389L)
                        ),
                        BigInteger.valueOf(0x3b9aca00L) // 1 Gwei
                )
        );

        assertEquals(
                new SenderImpl.FeeSuggestionImpl(
                        new BigDecimal("0.0000000015"), // 1.5 Gwei
                        new BigDecimal("0.000000076427423694") // 76 Gwei
                ),
                sugg
        );
    }
}
