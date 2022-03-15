package com.myodov.unicherrygarden.api.types.responseresult;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Any type containing the actual failure details of the request.
 */
public interface FailurePayload extends ResponsePayload {

    /**
     * Whether request has failed, and failure is common (may happen with different requests).
     */
    @JsonIgnore
    default boolean isCommonFailure() {
        return getType() == ResponsePayload.Type.FAILURE_COMMON;
    }

    /**
     * Whether request has failed, and failure is specific to the request.
     */
    @JsonIgnore
    default boolean isSpecificFailure() {
        return getType() == ResponsePayload.Type.FAILURE_SPECIFIC;
    }


    abstract class CommonFailurePayload implements FailurePayload {
        enum CommonFailureType {
            //            SPECIFIC, // This is not a common failure, but a request-specific one
            UNSPECIFIED_FAILURE,
            CHERRYGARDEN_NOT_READY,
            NO_RESPONSE_FROM_CHERRYGARDEN,
            CANCELLATION_COMPLETION_FAILURE,
            NODE_REQUEST_FAILURE
        }

        @Override
        public final Type getType() {
            return ResponsePayload.Type.FAILURE_COMMON;
        }

        /**
         * For a common failure only (when {@link #isCommonFailure()})
         */
        @JsonIgnore
        @NonNull
        abstract CommonFailureType getCommonFailureType();
    }

    final class UnspecifiedFailure extends CommonFailurePayload {

        @NonNull
        public final String msg;

        @JsonCreator
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

    // We want to be able to serialize this, no matter the class is empty.
    // Otherwise Jackson fails with a error like
    // "No serializer found for class ... and no properties discovered to create BeanSerializer
    // (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS)".
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    final class CherryGardenNotReadyFailure extends CommonFailurePayload {
        @Override
        @NonNull
        public CommonFailureType getCommonFailureType() {
            return CommonFailureType.CHERRYGARDEN_NOT_READY;
        }
    }

    @NonNull
    CherryGardenNotReadyFailure CHERRY_GARDEN_NOT_READY = new CherryGardenNotReadyFailure();


    // We want to be able to serialize this, no matter the class is empty.
    // Otherwise Jackson fails with a error like
    // "No serializer found for class ... and no properties discovered to create BeanSerializer
    // (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS)".
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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
    // We want to be able to serialize this, no matter the class is empty.
    // Otherwise Jackson fails with a error like
    // "No serializer found for class ... and no properties discovered to create BeanSerializer
    // (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS)".
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    final class CancellationCompletionFailure extends CommonFailurePayload {
        @Override
        @NonNull
        public CommonFailureType getCommonFailureType() {
            return CommonFailureType.CANCELLATION_COMPLETION_FAILURE;
        }
    }

    @NonNull
    CancellationCompletionFailure CANCELLATION_COMPLETION_FAILURE = new CancellationCompletionFailure();


    /**
     * There was a failure in connecting to the Ethereum node for the result.
     */
    // We want to be able to serialize this, no matter the class is empty.
    // Otherwise Jackson fails with a error like
    // "No serializer found for class ... and no properties discovered to create BeanSerializer
    // (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS)".
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    final class NodeRequestFailure extends CommonFailurePayload {
        @Override
        @NonNull
        public CommonFailureType getCommonFailureType() {
            return CommonFailureType.NODE_REQUEST_FAILURE;
        }
    }

    @NonNull
    NodeRequestFailure NODE_REQUEST_FAILURE = new NodeRequestFailure();

    abstract class SpecificFailurePayload implements FailurePayload {
        @Override
        public final Type getType() {
            return ResponsePayload.Type.FAILURE_SPECIFIC;
        }
    }
}
