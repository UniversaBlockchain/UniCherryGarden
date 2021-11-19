package com.myodov.unicherrygarden.api.types.dlt;

import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class MinedTxTest extends TxTest {
    final MinedTx MTX1 = new MinedTx(
            "0xb0b3d18c67857c30829e348987899026ee08232c989d60e47ccd78dca375d79a",
            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
            new Block(
                    13550555,
                    "0x4246574f55f6bb00326e17fa5ed6724df0b821babd3bf456cee2fd6a7b4dd25a",
                    Instant.ofEpochSecond(1636033366)),
            220);
    final MinedTx MTX1_COPY = new MinedTx(
            "0xb0b3d18c67857c30829e348987899026ee08232c989d60e47ccd78dca375d79a",
            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
            new Block(
                    13550555,
                    "0x4246574f55f6bb00326e17fa5ed6724df0b821babd3bf456cee2fd6a7b4dd25a",
                    Instant.ofEpochSecond(1636033366)),
            220);

    final Tx MTX1A = new MinedTx(
            "0xb0b3d18c67857c30829e348987899026ee08232c989d60e47ccd78dca375d79a",
            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
            new Block(
                    13550555,
                    "0x4246574f55f6bb00326e17fa5ed6724df0b821babd3bf456cee2fd6a7b4dd25a",
                    Instant.ofEpochSecond(1636033366)),
            220);

    final MinedTx MTX2 = new MinedTx(
            "0x17dd446c6901b78351a22007218391ead9d1a3c97ba6f0fa27c4b027ed099fd7",
            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
            new Block(
                    13550616,
                    "0xdc73aa4e8b3109e5215d541d4604b1fb700f4dcdb65efe3ac68b49568ddb1da7",
                    Instant.ofEpochSecond(1636034174)),
            204);
    final Tx MTX2A = new MinedTx(
            "0x17dd446c6901b78351a22007218391ead9d1a3c97ba6f0fa27c4b027ed099fd7",
            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
            new Block(
                    13550616,
                    "0xdc73aa4e8b3109e5215d541d4604b1fb700f4dcdb65efe3ac68b49568ddb1da7",
                    Instant.ofEpochSecond(1636034174)),
            204);


    @Test
    public void testEquals() {
        assertEquals("MinedTx with the same contents",
                MTX1,
                MTX1_COPY
        );
        assertEquals("MinedTx with the same contents - vice versa",
                MTX1_COPY,
                MTX1
        );

        assertNotEquals("MinedTx with different contents",
                MTX1,
                MTX2
        );
        assertNotEquals("MinedTx with different contents - vice versa",
                MTX2,
                MTX1
        );
    }

    @Test
    public void testEqualsCrossInheritance() {
        assertNotEquals("Tx and MinedTx with the same contents",
                TX1,
                MTX1
        );
        assertNotEquals("MinedTx and with the same contents - vice versa",
                MTX1,
                TX1
        );

        assertNotEquals("Tx and MinedTx (typed as Tx) with the same contents",
                TX1,
                MTX1A
        );
        assertNotEquals("MinedTx (typed as Tx) and with the same contents - vice versa",
                MTX1A,
                TX1
        );
    }
}
