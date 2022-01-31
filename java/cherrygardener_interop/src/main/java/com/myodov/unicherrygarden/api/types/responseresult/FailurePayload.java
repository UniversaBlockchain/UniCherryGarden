package com.myodov.unicherrygarden.api.types.responseresult;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Any type containing the actual failure details of the request.
 */
public interface FailurePayload extends ResponsePayload {

    abstract class CommonFailurePayload implements FailurePayload {
        enum CommonFailureType {
            //            SPECIFIC, // This is not a common failure, but a request-specific one
            UNSPECIFIED_FAILURE,
            NOT_AVAILABLE_IN_OFFLINE_MODE,
            NO_RESPONSE_FROM_CHERRYGARDEN,
            CANCELLATION_COMPLETION_FAILURE
        }

        @Override
        public final ResponseResult.@NonNull Type getType() {
            return ResponseResult.Type.FAILURE_COMMON;
        }

        @JsonIgnore
        @NonNull
        abstract CommonFailureType getCommonFailureType();
    }

    final class UnspecifiedFailure extends CommonFailurePayload {

        @NonNull
        public final String msg;

        public UnspecifiedFailure(@NonNull String msg) {
            this.msg = msg;
        }

        public UnspecifiedFailure() {
            this("");
        }

        @Override
        public String toString() {
            return String.format("UnspecifiedFailure(msg='%s')", msg);
        }

        @Override
        @NonNull
        public CommonFailureType getCommonFailureType() {
            return CommonFailureType.UNSPECIFIED_FAILURE;
        }
    }


    final class NotAvailableInOfflineMode extends CommonFailurePayload {
        @Override
        @NonNull
        public CommonFailureType getCommonFailureType() {
            return CommonFailureType.NOT_AVAILABLE_IN_OFFLINE_MODE;
        }
    }

    @NonNull
    NotAvailableInOfflineMode NOT_AVAILABLE_IN_OFFLINE_MODE = new NotAvailableInOfflineMode();

    final class NoResponseFromCherryGarden extends CommonFailurePayload {
        @Override
        @NonNull
        public CommonFailureType getCommonFailureType() {
            return CommonFailureType.NO_RESPONSE_FROM_CHERRYGARDEN;
        }
    }

    @NonNull
    NoResponseFromCherryGarden NO_RESPONSE_FROM_CHERRYGARDEN = new NoResponseFromCherryGarden();

    /**
     * There was a failure that caused either {@link java.util.concurrent.CancellationException}
     * or {@link java.util.concurrent.CompletionException} to be thrown.
     */
    final class CancellationCompletionFailure extends CommonFailurePayload {
        @Override
        @NonNull
        public CommonFailureType getCommonFailureType() {
            return CommonFailureType.CANCELLATION_COMPLETION_FAILURE;
        }
    }

    @NonNull
    CancellationCompletionFailure CANCELLATION_COMPLETION_FAILURE = new CancellationCompletionFailure();


    abstract class SpecificFailurePayload implements FailurePayload {
        @Override
        public final ResponseResult.@NonNull Type getType() {
            return ResponseResult.Type.FAILURE_SPECIFIC;
        }
    }
}
