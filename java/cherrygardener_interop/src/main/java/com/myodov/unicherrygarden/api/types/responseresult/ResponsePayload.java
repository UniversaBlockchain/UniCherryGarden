package com.myodov.unicherrygarden.api.types.responseresult;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.checkerframework.checker.nullness.qual.NonNull;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public interface ResponsePayload {
    enum Type {
        SUCCESS,
        FAILURE_COMMON,
        FAILURE_SPECIFIC
    }

    /**
     * Type of the result.
     * <p>
     * Most of the times you don’t want to use this method, use {@link #isSuccess} or {@link #isFailure} instead.
     */
    @JsonIgnore
    Type getType();

    /**
     * Whether request was successful.
     * <p>
     * Antonym: {@link #isFailure()}.
     */
    @JsonIgnore
    default boolean isSuccess() {
        return getType() == Type.SUCCESS;
    }

    /**
     * Whether request has failed.
     * <p>
     * Antonym: {@link #isSuccess()}.
     */
    @JsonIgnore
    default boolean isFailure() {
        final Type type = getType();
        return type == Type.FAILURE_COMMON || type == Type.FAILURE_SPECIFIC;
    }
}
