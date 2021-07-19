package com.myodov.unicherrygarden.cherrygardener.connector.impl;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class PrivateKeyImplTest {
    static final String PK1_STR = "f6e28eb945e0835f7ac0f0ccdaaf717f5700554e42ed21fc23580450f5243540";
    static final byte[] PK1 = Hex.decode(PK1_STR);
    static final String PK1_ADDR = "0x34e1E4F805fCdC936068A760b2C17BC62135b5AE";

    static final String PK2_STR = "42791cef1f565c4fe1e906e22a39174b46196fb9dc3c8ac56709fce193803b14";
    static final byte[] PK2 = Hex.decode(PK2_STR);
    static final String PK2_ADDR = "0x408a4ac0e80ba57210ea6a9ae6a9a7b687a51023";

    @Test
    public void testPrivateKeyImpl() {
        {  // private key 1
            final PrivateKeyImpl pk = new PrivateKeyImpl(PK1);

            assertEquals(
                    PK1_STR,
                    pk.getBytesHex()
            );

            assertArrayEquals(
                    PK1.clone(),
                    pk.getBytes()
            );

            assertEquals(
                    PK1_ADDR.toLowerCase(),
                    pk.getAddress()
            );

            assertEquals(
                    PK1_ADDR,
                    pk.getAddressEip55()
            );

            assertEquals(
                    "PrivateKeyImpl<0x34e1e4f805fcdc936068a760b2c17bc62135b5ae>",
                    pk.toString()
            );
        }
        {  // private key 2
            final PrivateKeyImpl pk = new PrivateKeyImpl(PK2);

            assertEquals(
                    PK2_STR,
                    pk.getBytesHex()
            );

            assertArrayEquals(
                    PK2.clone(),
                    pk.getBytes()
            );

            assertEquals(
                    PK2_ADDR.toLowerCase(),
                    pk.getAddress()
            );

            assertEquals(
                    PK2_ADDR,
                    pk.getAddressEip55()
            );

            assertEquals(
                    "PrivateKeyImpl<0x408a4ac0e80ba57210ea6a9ae6a9a7b687a51023>",
                    pk.toString()
            );
        }
    }
}
