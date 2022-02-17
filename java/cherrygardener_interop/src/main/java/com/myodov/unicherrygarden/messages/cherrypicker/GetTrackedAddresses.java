package com.myodov.unicherrygarden.messages.cherrypicker;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.ServiceKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload.CommonFailurePayload;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload.SpecificFailurePayload;
import com.myodov.unicherrygarden.api.types.responseresult.ResponsePayload;
import com.myodov.unicherrygarden.api.types.responseresult.SuccessPayload;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import com.myodov.unicherrygarden.messages.CherryGardenResponseWithPayload;
import com.myodov.unicherrygarden.messages.CherryPickerRequest;
import com.myodov.unicherrygarden.messages.RequestPayload;
import com.myodov.unicherrygarden.messages.RequestWithReplyTo;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Objects;

public class GetTrackedAddresses {
    @NonNull
    public static ServiceKey<Request> makeServiceKey(@NonNull String realm) {
        return ServiceKey.create(
                Request.class,
                String.format("%s:getTrackedAddressesService", Objects.requireNonNull(realm)));
    }


    public static final class GTARequestPayload
            implements RequestPayload {
        public final boolean includeComment;
        public final boolean includeSyncedFrom;

        @JsonCreator
        public GTARequestPayload(boolean includeComment,
                                 boolean includeSyncedFrom) {
            this.includeComment = includeComment;
            this.includeSyncedFrom = includeSyncedFrom;
        }

        @Override
        public String toString() {
            return String.format("GetTrackedAddresses.GTARequestPayload(%s, %s)",
                    includeComment, includeSyncedFrom);
        }
    }


    public static final class Request
            extends RequestWithReplyTo<GTARequestPayload, Response>
            implements CherryPickerRequest {
        @JsonCreator
        public Request(@NonNull ActorRef<Response> replyTo,
                       @NonNull GTARequestPayload payload) {
            super(replyTo, payload);
        }
    }

    public static class TrackedAddressesRequestResultPayload extends SuccessPayload {

        public static final class TrackedAddressInformation {
            @NonNull
            public final String address;

            @Nullable
            public final String comment;

            /**
             * If the syncedFrom data has been requested, it will be non-null.
             */
            @Nullable
            public final Integer syncedFrom;

            @JsonCreator
            public TrackedAddressInformation(@NonNull String address,
                                             @Nullable String comment,
                                             @Nullable Integer syncedFrom) {
                assert (address != null) && EthUtils.Addresses.isValidLowercasedAddress(address) : address;
                this.address = address;
                this.comment = comment;
                this.syncedFrom = syncedFrom;
            }

            @Override
            public String toString() {
                return String.format("GetTrackedAddresses.Response.TrackedAddressInformation(%s, %s, %s)",
                        address, comment, syncedFrom);
            }
        }


        /**
         * Results: the information about each tracked address.
         */
        @NonNull
        public final List<TrackedAddressInformation> addresses;

        /**
         * Whether the {@link #addresses} contain the comments for each address.
         * Copied from the request.
         */
        public final boolean includeComment;
        /**
         * Whether the {@link #addresses} contain the syncedFrom data for each address.
         * Copied from the request.
         */
        public final boolean includeSyncedFrom;

        /**
         * Constructor.
         */
        @JsonCreator
        public TrackedAddressesRequestResultPayload(@NonNull List<TrackedAddressInformation> addresses,
                                                    boolean includeComment,
                                                    boolean includeSyncedFrom) {
            assert addresses != null;
            this.addresses = addresses;
            this.includeComment = includeComment;
            this.includeSyncedFrom = includeSyncedFrom;
        }

        @Override
        public String toString() {
            return String.format("GetTrackedAddresses.TrackedAddressesRequestResult(%s, incComm=%s, incSyncFrom=%s)",
                    addresses, includeComment, includeSyncedFrom);
        }
    }

    public static class TrackedAddressesRequestResultFailure extends SpecificFailurePayload {
    }

    public static class Response
            extends CherryGardenResponseWithPayload<TrackedAddressesRequestResultPayload, TrackedAddressesRequestResultFailure> {

        @JsonCreator
        private Response(@NonNull ResponsePayload payload) {
            super(payload);
        }

        public Response(@NonNull TrackedAddressesRequestResultPayload payload) {
            this((ResponsePayload) payload);
        }

        public Response(@NonNull CommonFailurePayload commonFailure) {
            this((ResponsePayload) commonFailure);
        }

        public Response(@NonNull TrackedAddressesRequestResultFailure specificFailure) {
            this((ResponsePayload) specificFailure);
        }

        @NonNull
        public static Response fromCommonFailure(@NonNull CommonFailurePayload commonFailure) {
            assert commonFailure != null : commonFailure;
            return new Response(commonFailure);
        }
    }
}
