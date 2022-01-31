package com.myodov.unicherrygarden.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload.CommonFailurePayload;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload.SpecificFailurePayload;
import com.myodov.unicherrygarden.api.types.responseresult.ResponsePayload;
import com.myodov.unicherrygarden.api.types.responseresult.ResponseResult;
import com.myodov.unicherrygarden.api.types.responseresult.SuccessPayload;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Abstract generic response which contains a request result (<code>Res</code>), normally nullable.
 */
public class CherryGardenResponseWithResult<
        Data extends SuccessPayload,
        Failure extends SpecificFailurePayload>
        implements ResponseResult<Data, Failure>, CherryPickerResponse {

    @NonNull
    private final ResponsePayload payload;


    @JsonCreator
    protected CherryGardenResponseWithResult(@NonNull ResponsePayload payload) {
        // One, and only one, of `data`/`commonFailure`/`specificFailure`, must be non-null
        assert payload != null : payload;

        this.payload = payload;
    }

    public CherryGardenResponseWithResult(@NonNull Data data) {
        this((ResponsePayload) data);
    }

    public CherryGardenResponseWithResult(@NonNull CommonFailurePayload commonFailure) {
        this((ResponsePayload) commonFailure);
    }

    public CherryGardenResponseWithResult(@NonNull Failure specificFailure) {
        this((ResponsePayload) specificFailure);
    }

    @Override
    public String toString() {
        return String.format("%s.%s(%s)",
                getClass().getEnclosingClass().getSimpleName(), getClass().getSimpleName(),
                payload);
    }

    @Override
    public @NonNull Type getType() {
        return payload.getType();
    }

    @Override
    @NonNull
    public ResponsePayload getPayload() {
        return payload;
    }

    @Override
    @NonNull
    public Data getSuccessfulPayload() {
        return (Data) payload;
    }

    @Override
    @NonNull
    public CommonFailurePayload getCommonFailure() {
        return (CommonFailurePayload) payload;
    }

    @Override
    @NonNull
    public Failure getSpecificFailure() {
        return (Failure) payload;
    }
}
