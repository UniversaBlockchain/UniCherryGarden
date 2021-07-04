package com.myodov.unicherrygarden.cherrygardener.connector.impl;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class PrivateKeyImplTest {

    private static final String PK1_STR = "f6e28eb945e0835f7ac0f0ccdaaf717f5700554e42ed21fc23580450f5243540";
    private static final byte[] PK1 = Hex.decode("f6e28eb945e0835f7ac0f0ccdaaf717f5700554e42ed21fc23580450f5243540");

    @Test
    public void testPrivateKeyImpl() {
        final PrivateKeyImpl pk1 = new PrivateKeyImpl(PK1);

        assertEquals(
                PK1_STR,
                pk1.getBytesHex()
        );

        assertArrayEquals(
                PK1.clone(),
                pk1.getBytes()
        );

        assertEquals(
                "0x34e1e4f805fcdc936068a760b2c17bc62135b5ae",
                pk1.getAddress()
        );

        assertEquals(
                "0x34e1E4F805fCdC936068A760b2C17BC62135b5AE",
                pk1.getAddressEip55()
        );

        assertEquals(
                "PrivateKeyImpl<0x34e1e4f805fcdc936068a760b2c17bc62135b5ae>",
                pk1.toString()
        );
    }
}
