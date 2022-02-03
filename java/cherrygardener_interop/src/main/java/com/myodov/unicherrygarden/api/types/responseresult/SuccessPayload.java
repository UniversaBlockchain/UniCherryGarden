package com.myodov.unicherrygarden.api.types.responseresult;

/**
 * Any type containing the actual data of the request.
 */
public abstract class SuccessPayload implements ResponsePayload {
    @Override
    public final Type getType() {
        return ResponsePayload.Type.SUCCESS;
    }
}
