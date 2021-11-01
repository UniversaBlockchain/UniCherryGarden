package com.myodov.unicherrygarden.ethereum;

import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class EthUtilsUint256StrTest {
    @Test
    public void testToAddress() {
        assertEquals(
                "Should work well with proper address",
                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                EthUtils.Uint256Str.toAddress("0x000000000000000000000000d701edf8f9c5d834bcb9add73ddeff2d6b9c3d24")
        );
        assertThrows(
                "Should fail if the hex string contains non-zeroes in high bytes",
                IllegalArgumentException.class,
                () -> EthUtils.Uint256Str.toAddress("0x000000000000000000000001d701edf8f9c5d834bcb9add73ddeff2d6b9c3d24")
        );
    }

    @Test
    public void testFromAddress() {
        assertEquals(
                "Should work well with proper address",
                "0x000000000000000000000000d701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                EthUtils.Uint256Str.fromAddress("0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24")
        );
        assertThrows(
                "Should fail if the hex string is anything but non-hex",
                IllegalArgumentException.class,
                () -> EthUtils.Uint256Str.fromAddress(" 0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d")
        );
        assertThrows(
                "Should fail if the hex string is valid address of wrong length",
                IllegalArgumentException.class,
                () -> EthUtils.Uint256Str.fromAddress("0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d2")
        );
        assertThrows(
                "Should fail even if the hex string is not lowercased",
                IllegalArgumentException.class,
                () -> EthUtils.Uint256Str.fromAddress("0xD701edf8f9c5d834bcb9add73ddeff2d6b9c3d24")
        );
    }

    @Test
    public void testToBigInteger() {
        assertEquals(
                "Should work well with proper number",
                new BigInteger("25017000000000000000000"),
                EthUtils.Uint256Str.toBigInteger("0x00000000000000000000000000000000000000000000054c2c9e1a40db440000")
        );
    }
}
