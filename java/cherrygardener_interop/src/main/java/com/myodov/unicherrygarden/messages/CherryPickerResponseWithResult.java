package com.myodov.unicherrygarden.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Abstract generic response which contains a request result (<code>Res</code>), normally nullable.
 */
public class CherryPickerResponseWithResult<Res> implements CherryPickerResponse {
    @Nullable
    public final Res result;

    @JsonCreator
    public CherryPickerResponseWithResult(@Nullable Res result) {
        this.result = result;
    }

    @Nullable
    public final Res getResult() {
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s.%s(%s)",
                getClass().getEnclosingClass().getSimpleName(), getClass().getSimpleName(),
                result);
    }
}
