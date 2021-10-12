package com.myodov.unicherrygarden.messages.cherrypicker;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.ServiceKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import com.myodov.unicherrygarden.messages.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;


public class GetTrackedAddresses {
    @NonNull
    public static final ServiceKey<Request> SERVICE_KEY =
            ServiceKey.create(Request.class, "getTrackedAddressesService");


    public static final class GTARequestPayload
            implements RequestPayload {
        public final boolean includeComment;
        public final boolean includeSyncedFrom;
        public final boolean includeSyncedTo;

        @JsonCreator
        public GTARequestPayload(boolean includeComment,
                                 boolean includeSyncedFrom,
                                 boolean includeSyncedTo) {
            this.includeComment = includeComment;
            this.includeSyncedFrom = includeSyncedFrom;
            this.includeSyncedTo = includeSyncedTo;
        }

        @Override
        public String toString() {
            return String.format("GetTrackedAddresses.GTARequestPayload(%s, %s, %s)",
                    includeComment, includeSyncedFrom, includeSyncedTo);
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


    public static final class Response
            implements CherryPickerResponse {

        public static final class TrackedAddressInformation
                implements Serializable {
            @NonNull
            public final String address;

            @Nullable
            public final String comment;

            /**
             * If the syncedFrom data has been requested, it will be non-null.
             */
            @Nullable
            public final Integer syncedFrom;

            @Nullable
            public final Integer syncedTo;

            @JsonCreator
            public TrackedAddressInformation(@NonNull String address,
                                             @Nullable String comment,
                                             @Nullable Integer syncedFrom,
                                             @Nullable Integer syncedTo) {
                assert (address != null) && EthUtils.Addresses.isValidLowercasedAddress(address) : address;
                this.address = address;
                this.comment = comment;
                this.syncedFrom = syncedFrom;
                this.syncedTo = syncedTo;
            }

            @Override
            public String toString() {
                return String.format("GetTrackedAddresses.Response.TrackedAddressInformation(%s, %s, %s, %)",
                        address, comment, syncedFrom, syncedTo);
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
         * Whether the {@link #addresses} contain the syncedTo data for each address.
         * Copied from the request.
         */
        public final boolean includeSyncedTo;

        @JsonCreator
        public Response(@NonNull List<TrackedAddressInformation> addresses,
                        boolean includeComment,
                        boolean includeSyncedFrom,
                        boolean includeSyncedTo) {
            assert addresses != null;
            this.addresses = addresses;
            this.includeComment = includeComment;
            this.includeSyncedFrom = includeSyncedFrom;
            this.includeSyncedTo = includeSyncedTo;
        }

        @Override
        public String toString() {
            return String.format("GetTrackedAddresses.Response(%s, incComm=%s, incSyncFrom=%s, incSyncTo=%s)",
                    addresses, includeComment, includeSyncedFrom, includeSyncedTo);
        }
    }
}
