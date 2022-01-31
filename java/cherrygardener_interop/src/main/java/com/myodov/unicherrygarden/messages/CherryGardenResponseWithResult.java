package com.myodov.unicherrygarden.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.api.types.UniCherryGardenError;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload.CommonFailurePayload;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload.SpecificFailurePayload;
import com.myodov.unicherrygarden.api.types.responseresult.ResponseResult;
import com.myodov.unicherrygarden.api.types.responseresult.SuccessPayload;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Abstract generic response which contains a request result (<code>Res</code>), normally nullable.
 */
public class CherryGardenResponseWithResult<
        Data extends SuccessPayload,
        Failure extends SpecificFailurePayload>
        implements ResponseResult<Data, Failure>, CherryPickerResponse {

    @Nullable
    private final Data data;

    @Nullable
    private final CommonFailurePayload commonFailure;

    @Nullable
    private final Failure specificFailure;

    @JsonCreator
    public CherryGardenResponseWithResult(@Nullable Data data,
                                          @Nullable CommonFailurePayload commonFailure,
                                          @Nullable Failure specificFailure) {
        // One, and only one, of `data`/`commonFailure`/`specificFailure`, must be non-null
        assert (data != null && commonFailure == null && specificFailure == null) ||
                (data == null && commonFailure != null && specificFailure == null) ||
                (data == null && commonFailure == null && specificFailure != null) :
                String.format("%s/%s/%s", data, commonFailure, specificFailure);

        this.data = data;
        this.commonFailure = commonFailure;
        this.specificFailure = specificFailure;
    }

    public CherryGardenResponseWithResult(@NonNull Data data) {
        this(data, null, null);
    }

    public CherryGardenResponseWithResult(@NonNull CommonFailurePayload commonFailure) {
        this(null, commonFailure, null);
    }

    public CherryGardenResponseWithResult(@NonNull Failure specificFailure) {
        this(null, null, specificFailure);
    }

    @Override
    public String toString() {
        return String.format("%s.%s(%s/%s/%s)",
                getClass().getEnclosingClass().getSimpleName(), getClass().getSimpleName(),
                data, commonFailure, specificFailure);
    }

    @Override
    public @NonNull Type getType() {
        if (data != null) {
            return Type.SUCCESS;
        } else if (commonFailure != null) {
            return Type.FAILURE_COMMON;
        } else if (specificFailure != null) {
            return Type.FAILURE_SPECIFIC;
        } else {
            final String msg = String.format("Unknown response type: %s", this);
            throw new UniCherryGardenError(msg);
        }
    }

    @Override
    public Data getPayload() {
        return data;
    }

    @Override
    public FailurePayload getCommonFailure() {
        return commonFailure;
    }

    @Override
    public Failure getSpecificFailure() {
        return specificFailure;
    }
}
