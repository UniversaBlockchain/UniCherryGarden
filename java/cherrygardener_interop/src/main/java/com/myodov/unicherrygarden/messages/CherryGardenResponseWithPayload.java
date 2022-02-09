package com.myodov.unicherrygarden.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.api.types.UniCherryGardenFailure;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload.CommonFailurePayload;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload.SpecificFailurePayload;
import com.myodov.unicherrygarden.api.types.responseresult.ResponsePayload;
import com.myodov.unicherrygarden.api.types.responseresult.ResponseWithPayload;
import com.myodov.unicherrygarden.api.types.responseresult.SuccessPayload;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Abstract generic response which contains a request result (<code>Res</code>), normally nullable.
 */
public class CherryGardenResponseWithPayload<
        Data extends SuccessPayload,
        Failure extends SpecificFailurePayload>
        implements ResponseWithPayload<Data, Failure>, CherryPickerResponse {

    @NonNull
    private final ResponsePayload payload;


    @JsonCreator
    protected CherryGardenResponseWithPayload(@NonNull ResponsePayload payload) {
        // One, and only one, of `data`/`commonFailure`/`specificFailure`, must be non-null
        assert payload != null : payload;

        this.payload = payload;
    }

    public CherryGardenResponseWithPayload(@NonNull Data data) {
        this((ResponsePayload) data);
    }

    public CherryGardenResponseWithPayload(@NonNull CommonFailurePayload commonFailure) {
        this((ResponsePayload) commonFailure);
    }

    public CherryGardenResponseWithPayload(@NonNull Failure specificFailure) {
        this((ResponsePayload) specificFailure);
    }

    @Override
    public String toString() {
        return String.format("%s.%s(%s)",
                getClass().getEnclosingClass().getSimpleName(), getClass().getSimpleName(),
                payload);
    }

    @Override
    @NonNull
    public ResponsePayload getPayload() {
        return payload;
    }

    @Override
    @NonNull
    @SuppressWarnings("unchecked") // it is your duty to ensure it is castable
    public Data getPayloadAsSuccessful() throws UniCherryGardenFailure {
        if (isSuccess()) {
            return (Data) payload;
        } else {
            throw new UniCherryGardenFailure((FailurePayload) payload);
        }
    }

    @Override
    @NonNull
    @SuppressWarnings("unchecked") // it is your duty to ensure it is castable
    public CommonFailurePayload getCommonFailure() {
        return (CommonFailurePayload) payload;
    }

    @Override
    @NonNull
    @SuppressWarnings("unchecked") // it is your duty to ensure it is castable
    public Failure getSpecificFailure() {
        return (Failure) payload;
    }
}
