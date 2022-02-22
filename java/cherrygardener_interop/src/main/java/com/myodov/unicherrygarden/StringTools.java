package com.myodov.unicherrygarden;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Various convenience methods to deal with strings.
 */
public class StringTools {
    /**
     * Get the inner argument as a string (converting it via <code>toString</code>);
     * or, if the argument is <code>null</code> – return <code>N/A</code>.
     */
    @NonNull
    public static <T> String naIfNull(@Nullable T stringable) {
        return (stringable == null) ? "N/A" : stringable.toString();
    }

    /**
     * Add offset to some (probably multi-line) string.
     */
    @NonNull
    public static String withOffset(int offset, @NonNull String string) {
        assert offset >= 0 : offset;
        assert string != null;
        return Arrays.stream(string.split("\n"))
                .map(l -> (offset > 0) ? String.format("%" + offset + "s%s", "", l) : l)
                .collect(Collectors.joining("\n"));
    }
}
