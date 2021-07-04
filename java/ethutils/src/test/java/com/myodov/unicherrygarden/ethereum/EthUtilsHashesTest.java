package com.myodov.unicherrygarden.ethereum;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.Assert.*;

public class EthUtilsHashesTest {
    @Test
    public void testIsValidHash() {
        assertTrue(
                "Correct hash",
                EthUtils.isValidHash("0x7150afaf4bdc777e4c75de0bc15c34a23f394f8bf391cfcb7dd6309f1b14b535", 66)
        );
        assertFalse(
                "Shorter than needed",
                EthUtils.isValidHash("0x7150afaf4bdc777e4c75de0bc15c34a23f394f8bf391cfcb7dd6309f1b14b53", 66)
        );
        assertFalse(
                "Longer than needed",
                EthUtils.isValidHash("0x7150afaf4bdc777e4c75de0bc15c34a23f394f8bf391cfcb7dd6309f1b14b5357", 66)
        );
        assertFalse(
                "No leading 0x prefix",
                EthUtils.isValidHash("7150afaf4bdc777e4c75de0bc15c34a23f394f8bf391cfcb7dd6309f1b14b5357", 66)
        );
        assertFalse(
                "No leading 0x prefix (and correct length)",
                EthUtils.isValidHash("307150afaf4bdc777e4c75de0bc15c34a23f394f8bf391cfcb7dd6309f1b14b5357", 66)
        );
        assertFalse(
                "Lowercase only",
                EthUtils.isValidHash("0x7150Afaf4bdc777e4c75de0bc15c34a23f394f8bf391cfcb7dd6309f1b14b535", 66)
        );
        assertFalse(
                "Even the prefix is in lowercase",
                EthUtils.isValidHash("0X7150afaf4bdc777e4c75de0bc15c34a23f394f8bf391cfcb7dd6309f1b14b535", 66)
        );
    }

    @Test
    public void testIsValidBlockHash() {
        assertTrue(
                EthUtils.Hashes.isValidBlockHash("0x7150afaf4bdc777e4c75de0bc15c34a23f394f8bf391cfcb7dd6309f1b14b535")
        );
    }

    @Test
    public void testIsValidTransactionHash() {
        assertTrue(
                EthUtils.Hashes.isValidTransactionHash("0x86e671138ae2d393ee0041358ded3b0f843352ce4d7191d627452d946f3e28b2")
        );
    }
}
