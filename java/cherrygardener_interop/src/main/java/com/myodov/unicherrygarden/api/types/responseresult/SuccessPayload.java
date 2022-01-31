package com.myodov.unicherrygarden.api.types.responseresult;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Any type containing the actual data of the request.
 */
public abstract class SuccessPayload implements ResponsePayload {
    @Override
    public final ResponseResult.@NonNull Type getType() {
        return ResponseResult.Type.SUCCESS;
    }
}
