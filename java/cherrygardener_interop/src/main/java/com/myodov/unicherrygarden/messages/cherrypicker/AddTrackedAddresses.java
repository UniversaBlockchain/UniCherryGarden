package com.myodov.unicherrygarden.messages.cherrypicker;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.ServiceKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import com.myodov.unicherrygarden.messages.CherryPickerRequest;
import com.myodov.unicherrygarden.messages.CherryPickerResponse;
import com.myodov.unicherrygarden.messages.RequestPayload;
import com.myodov.unicherrygarden.messages.RequestWithReplyTo;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;


public class AddTrackedAddresses {
    @NonNull
    public static final ServiceKey<Request> SERVICE_KEY =
            ServiceKey.create(Request.class, "addTrackedAddressesService");


    /**
     * From what block should we start tracking an address/addresses.
     */
    public enum StartTrackingAddressMode {
        /**
         * Track an address from a block with specific number.
         */
        FROM_BLOCK,
        /**
         * Track an address from a latest block known to the Ethereum node.
         */
        LATEST_KNOWN_BLOCK,
        /**
         * Track an address from a latest block successfully synced by the Ethereum node.
         */
        // LATEST_NODE_SYNCED_BLOCK, // TODO: add this option later
        /**
         * Track an address from a latest block fully synced by UniCherryGarden.
         */
        // LATEST_CHERRYGARDEN_SYNCED_BLOCK, // TODO: add this option later
    }

    public static final class AddressDataToTrack {
        @NonNull
        public final String address;
        @Nullable
        public final String comment;
        public final int fromBlock;

        /**
         * Constructor.
         *
         * @param fromBlock The number of blockchain block from which to track the address.
         *                  Is relevant only if if the {@link StartTrackingAddressMode} for the whole set of addresses
         *                  is set to {@link StartTrackingAddressMode#FROM_BLOCK};
         *                  otherwise should be <code>0</code>.
         */
        @JsonCreator
        public AddressDataToTrack(@NonNull String address,
                                  @Nullable String comment,
                                  int fromBlock) {
            assert (address != null) && EthUtils.Addresses.isValidLowercasedAddress(address) : address;
            assert fromBlock >= 0 : fromBlock;
            this.address = address;
            this.comment = comment;
            this.fromBlock = fromBlock;
        }

        @Override
        public String toString() {
            return String.format("AddTrackedAddresses.AddressDataToTrack(%s, %s, %s)", address, comment, fromBlock);
        }
    }

    public static final class ATARequestPayload
            implements RequestPayload {
        @NonNull
        public final StartTrackingAddressMode trackingMode;
        @NonNull
        public final List<AddressDataToTrack> addressesToTrack;

        @JsonCreator
        public ATARequestPayload(@NonNull StartTrackingAddressMode trackingMode,
                                 @NonNull List<AddressDataToTrack> addressesToTrack) {
            assert trackingMode != null;
            assert addressesToTrack != null;
            this.trackingMode = trackingMode;
            this.addressesToTrack = addressesToTrack;
        }

        @Override
        public String toString() {
            return String.format("AddTrackedAddresses.ATARequestPayload(%s, %s)", trackingMode, addressesToTrack);
        }
    }


    public static final class Request
            extends RequestWithReplyTo<ATARequestPayload, Response>
            implements CherryPickerRequest {
        @JsonCreator
        public Request(@NonNull ActorRef<Response> replyTo,
                       @NonNull ATARequestPayload payload) {
            super(replyTo, payload);
        }
    }


    public static final class Response implements CherryPickerResponse {
        /**
         * The Ethereum addresses (lowercased) that were successfully added to tracking.
         * Stored as {@link Set} so they are definitely unique/non-repeating.
         */
        @NonNull
        public final Set<String> addresses;

        @JsonCreator
        public Response(@NonNull Set<String> addresses) {
            assert addresses != null;
            assert addresses.stream().allMatch(addr -> (addr != null) && EthUtils.Addresses.isValidLowercasedAddress(addr))
                    : addresses;
            this.addresses = Collections.unmodifiableSet(addresses);
        }

        @Override
        public String toString() {
            return String.format("AddTrackedAddresses.Response(%s)", addresses);
        }
    }
}
