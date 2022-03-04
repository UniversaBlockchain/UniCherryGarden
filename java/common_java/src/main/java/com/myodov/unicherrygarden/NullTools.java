package com.myodov.unicherrygarden;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Various convenience methods to deal with nullable elements.
 */
public class NullTools {
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

    /**
     * Find the first of items which is not null; or return null.
     */
    @Nullable
    @SafeVarargs
    public static <T> T coalesce(T... items) {
        for (@Nullable final T item : items) {
            if (item != null) {
                return item;
            }
        }
        return null;
    }
}
