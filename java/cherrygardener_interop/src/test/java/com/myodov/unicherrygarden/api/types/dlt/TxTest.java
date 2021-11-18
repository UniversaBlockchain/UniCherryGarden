package com.myodov.unicherrygarden.api.types.dlt;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TxTest {
    final Tx TX1 = new Tx(
            "0xb0b3d18c67857c30829e348987899026ee08232c989d60e47ccd78dca375d79a",
            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7");
    final Tx TX1_COPY = new Tx(
            "0xb0b3d18c67857c30829e348987899026ee08232c989d60e47ccd78dca375d79a",
            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7");

    final Tx TX2 = new Tx(
            "0x17dd446c6901b78351a22007218391ead9d1a3c97ba6f0fa27c4b027ed099fd7",
            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7"
    );

    @Test
    public void testEquals() {
        assertEquals(
                "Tx with the same contents",
                TX1,
                TX1_COPY
        );
        assertEquals(
                "Tx with the same contents - vice versa",
                TX1_COPY,
                TX1
        );

        assertNotEquals(
                "Tx with different contents",
                TX1,
                TX2
        );
        assertNotEquals(
                "Tx with different contents - vice versa",
                TX2,
                TX1
        );
    }
}
