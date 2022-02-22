package com.myodov.unicherrygarden;

import org.junit.Test;

import java.math.BigDecimal;

import static com.myodov.unicherrygarden.StringTools.naIfNull;
import static com.myodov.unicherrygarden.StringTools.withOffset;
import static org.junit.Assert.assertEquals;

public class StringToolsTest {
    @Test
    public void testNaIfNull() {
        assertEquals(
                "Hi",
                naIfNull("Hi")
        );

        assertEquals(
                "0",
                naIfNull(BigDecimal.ZERO)
        );

        assertEquals(
                "N/A",
                naIfNull(null)
        );
    }

    @Test
    public void testAddOffset() {
        assertEquals(
                "Line 1",
                withOffset(0, "Line 1")
        );

        assertEquals(
                "Line 1\n" +
                        "Line2\n" +
                        "Line3",
                withOffset(0,
                        "Line 1\n" +
                                "Line2\n" +
                                "Line3")
        );

        assertEquals(
                "  Line 1",
                withOffset(2, "Line 1")
        );

        assertEquals(
                "  Line 1\n" +
                        "  Line2\n" +
                        "  Line3",
                withOffset(2,
                        "Line 1\n" +
                                "Line2\n" +
                                "Line3")
        );
    }
}
