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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;


public class AddTrackedAddresses {
    @NonNull
    public static ServiceKey<Request> makeServiceKey(@NonNull String realm) {
        return ServiceKey.create(
                Request.class,
                String.format("%s:addTrackedAddressesService", Objects.requireNonNull(realm)));
    }


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
        LATEST_NODE_SYNCED_BLOCK,
        /**
         * Track an address from a latest block fully synced by UniCherryGarden.
         */
        LATEST_CHERRYGARDEN_SYNCED_BLOCK,
    }

    public static final class AddressDataToTrack {
        @NonNull
        public final String address;
        @Nullable
        public final String comment;

        /**
         * Constructor.
         */
        @JsonCreator
        public AddressDataToTrack(@NonNull String address,
                                  @Nullable String comment) {
            assert (address != null) && EthUtils.Addresses.isValidLowercasedAddress(address) : address;
            this.address = address;
            this.comment = comment;
        }

        @Override
        public String toString() {
            return String.format("%s.%s(%s, %s)",
                    getClass().getEnclosingClass().getSimpleName(), getClass().getSimpleName(),
                    address, comment);
        }
    }

    public static final class ATARequestPayload
            implements RequestPayload {
        @NonNull
        public final StartTrackingAddressMode trackingMode;
        @NonNull
        public final List<AddressDataToTrack> addressesToTrack;
        @Nullable
        public final Integer fromBlock;

        /**
         * Constructor.
         *
         * @param fromBlock The number of blockchain block from which to track the address.
         *                  Is relevant only if if the {@link StartTrackingAddressMode} for the whole set of addresses
         *                  is set to {@link StartTrackingAddressMode#FROM_BLOCK};
         *                  otherwise should be <code>0</code>.
         */
        @JsonCreator
        public ATARequestPayload(@NonNull StartTrackingAddressMode trackingMode,
                                 @NonNull List<AddressDataToTrack> addressesToTrack,
                                 @Nullable Integer fromBlock) {
            assert trackingMode != null;
            assert addressesToTrack != null;
            assert (trackingMode == AddTrackedAddresses.StartTrackingAddressMode.FROM_BLOCK) == (fromBlock != null)
                    :
                    String.format("%s:%s", trackingMode, fromBlock);
            assert (fromBlock == null) || (fromBlock.intValue() >= 0) : fromBlock;
            this.trackingMode = trackingMode;
            this.addressesToTrack = addressesToTrack;
            this.fromBlock = fromBlock;
        }

        @Override
        public String toString() {
            return String.format("%s(%s, %s, %s)",
                    getClass().getSimpleName(),
                    trackingMode, addressesToTrack, fromBlock);
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

    public static class AddTrackedAddressesRequestResultPayload extends SuccessPayload {
        /**
         * The Ethereum addresses (lowercased) that were successfully added to tracking.
         * Stored as {@link Set} so they are definitely unique/non-repeating.
         */
        @NonNull
        public final Set<String> addresses;

        @JsonCreator
        public AddTrackedAddressesRequestResultPayload(@NonNull Set<String> addresses) {
            assert addresses != null;
            assert addresses.stream().allMatch(addr -> (addr != null) && EthUtils.Addresses.isValidLowercasedAddress(addr))
                    : addresses;
            this.addresses = Collections.unmodifiableSet(addresses);
        }
    }

    public static class AddTrackedAddressesRequestResultFailure extends SpecificFailurePayload {
    }

    public static final class Response
            extends CherryGardenResponseWithPayload<AddTrackedAddressesRequestResultPayload, AddTrackedAddressesRequestResultFailure> {

        @JsonCreator
        private Response(@NonNull ResponsePayload payload) {
            super(payload);
        }

        public Response(@NonNull AddTrackedAddressesRequestResultPayload payload) {
            this((ResponsePayload) payload);
        }

        public Response(@NonNull CommonFailurePayload commonFailure) {
            this((ResponsePayload) commonFailure);
        }

        public Response(@NonNull AddTrackedAddressesRequestResultFailure specificFailure) {
            this((ResponsePayload) specificFailure);
        }

        @NonNull
        public static Response fromCommonFailure(@NonNull CommonFailurePayload commonFailure) {
            assert commonFailure != null : commonFailure;
            return new Response(commonFailure);
        }
    }
}
