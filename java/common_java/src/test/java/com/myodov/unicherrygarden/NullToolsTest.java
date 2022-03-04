package com.myodov.unicherrygarden;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.myodov.unicherrygarden.NullTools.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NullToolsTest {
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

    @Test
    public void testCoalesce() {
        final String s = null;
        final Long l = null;

        assertNull(coalesce());
        assertNull(coalesce(s));
        assertNull(coalesce(s, s));

        assertEquals(
                "a",
                coalesce(s, "a", "b", s)
        );
        assertEquals(
                "a",
                coalesce(s, s, s, "a", "b", s)
        );

        assertEquals(
                Long.valueOf(3L),
                coalesce(l, l, l, 3L, l, 7L, l)
        );
    }
}
