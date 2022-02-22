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
import java.util.Set;

public class GetTrackedAddresses {
    @NonNull
    public static ServiceKey<Request> makeServiceKey(@NonNull String realm) {
        return ServiceKey.create(
                Request.class,
                String.format("%s:getTrackedAddressesService", Objects.requireNonNull(realm)));
    }


    public static final class GTARequestPayload
            implements RequestPayload {

        @Nullable
        public final Set<String> filterAddresses;
        public final boolean includeComment;
        public final boolean includeSyncedFrom;

        @JsonCreator
        public GTARequestPayload(@Nullable Set<String> filterAddresses,
                                 boolean includeComment,
                                 boolean includeSyncedFrom) {
            assert filterAddresses == null ||
                    filterAddresses
                            .stream()
                            .allMatch(addr -> (addr != null) && EthUtils.Addresses.isValidLowercasedAddress(addr))
                    :
                    filterAddresses;

            this.filterAddresses = filterAddresses;
            this.includeComment = includeComment;
            this.includeSyncedFrom = includeSyncedFrom;
        }

        @Override
        public String toString() {
            return String.format("%s(%s, %s, %s)",
                    getClass().getSimpleName(),
                    filterAddresses, includeComment, includeSyncedFrom);
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
                return String.format("%s.%s(%s, %s, %s)",
                        getClass().getEnclosingClass().getSimpleName(), getClass().getSimpleName(),
                        address, comment, syncedFrom);
            }

            @NonNull
            public String toHumanString() {
                return String.format(
                        "%s%s",
                        address,
                        (comment != null) ? "" : String.format("(%s)", comment)
                );
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
            return String.format("%s(%s, incComm=%s, incSyncFrom=%s)",
                    getClass().getSimpleName(),
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
