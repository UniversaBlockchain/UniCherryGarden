package com.myodov.unicherrygarden.messages.cherrypicker;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.ServiceKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import com.myodov.unicherrygarden.messages.CherryPickerRequest;
import com.myodov.unicherrygarden.messages.CherryPickerResponse;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;


public class GetTrackedAddresses {
    public static final @NonNull ServiceKey<Request> SERVICE_KEY =
            ServiceKey.create(Request.class, "getTrackedAddressesService");

    public static final class Request implements CherryPickerRequest {
        @NonNull
        public final ActorRef<Response> replyTo;
        public final boolean includeComment;
        public final boolean includeSyncedFrom;
        public final boolean includeSyncedTo;

        @JsonCreator
        public Request(@NonNull ActorRef<Response> replyTo,
                       boolean includeComment,
                       boolean includeSyncedFrom,
                       boolean includeSyncedTo) {
            this.replyTo = replyTo;
            this.includeComment = includeComment;
            this.includeSyncedFrom = includeSyncedFrom;
            this.includeSyncedTo = includeSyncedTo;
        }

        @Override
        public String toString() {
            return String.format("GetTrackedAddresses.Request(%s)", replyTo);
        }
    }

    public static final class Response implements CherryPickerResponse {

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
            this.addresses = addresses;
            this.includeComment = includeComment;
            this.includeSyncedFrom = includeSyncedFrom;
            this.includeSyncedTo = includeSyncedTo;
        }

        @Override
        public String toString() {
            return String.format("GetTrackedAddresses.Response(%s: %s, incComm=%s, incSyncFrom=%s, incSyncTo=%s)",
                    addresses, includeComment, includeSyncedFrom, includeSyncedTo);
        }
    }
}
