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

import java.util.Objects;

public class GetAddressDetails {
    @NonNull
    public static ServiceKey<Request> makeServiceKey(@NonNull String realm) {
        return ServiceKey.create(
                Request.class,
                String.format("%s:getAddressDetailsService", Objects.requireNonNull(realm)));
    }


    public static final class GADRequestPayload
            implements RequestPayload {

        @NonNull
        public String address;

        @JsonCreator
        public GADRequestPayload(@NonNull String address) {
            assert address != null && EthUtils.Addresses.isValidLowercasedAddress(address) : address;

            this.address = address;
        }

        @Override
        public String toString() {
            return String.format("GetAddressDetails.GADRequestPayload(%s)",
                    address);
        }
    }


    public static final class Request
            extends RequestWithReplyTo<GADRequestPayload, Response>
            implements CherryPickerRequest {
        @JsonCreator
        public Request(@NonNull ActorRef<Response> replyTo,
                       @NonNull GADRequestPayload payload) {
            super(replyTo, payload);
        }
    }


    public static class AddressDetailsRequestResultPayload extends SuccessPayload {

        public static final class AddressDetails {

            public static final class Nonces {
                /**
                 * Next nonce we should use, if we make the next transaction after all the transactions
                 * stored in the blockchain.
                 * <p>
                 * Equivalent of `eth.getTransactionCount(address)` or, more explicitly,
                 * of `eth.getTransactionCount(address, 'latest')`.
                 * <p>
                 * If there are no transactions stored in the blockchain, it will (reasonably and expectedly)
                 * contain <code>0</code>.
                 */
                public final int nextInBlockchain;

                /**
                 * Next nonce we should use, if we make the next transaction after all the transactions
                 * registered in the pending pool.
                 * Equivalent of `eth.getTransactionCount(address, 'pending')`.
                 * <p>
                 * Contains <code>null</code> if CherryPicker doesn’t see any outgoing transactions
                 * from this address in pending pool.
                 * In this case you should fallback to {@link #nextInBlockchain}.
                 */
                @Nullable
                public final Integer nextInPendingPool;

                /**
                 * Next nonce we should use, if we make the next transaction after all the transactions
                 * that we are already planting by CherryPlanter.
                 * <p>
                 * Contains <code>null</code> if nothing is being planted by CherryPlanter.
                 * In this case you should fallback to {@link #nextInPendingPool} and {@link #nextInBlockchain}.
                 */
                @Nullable
                public final Integer nextPlanting;

                @JsonCreator
                public Nonces(int nextInBlockchain,
                              @Nullable Integer nextInPendingPool,
                              @Nullable Integer nextPlanting) {
                    // Nonce should be 0 <= nonce <= 2^64-1 per EIP-2681
                    assert nextInBlockchain >= 0 : nextInBlockchain;
                    assert nextInPendingPool == null || nextInPendingPool >= 0 : nextInPendingPool;
                    assert nextPlanting == null || nextPlanting >= 0 : nextPlanting;

                    this.nextInBlockchain = nextInBlockchain;
                    this.nextInPendingPool = nextInPendingPool;
                    this.nextPlanting = nextPlanting;
                }

                @Override
                public String toString() {
                    return String.format("GetAddressDetails.AddressDetailsRequestResultPayload.AddressDetails.Nonces(%s, %s, %s)",
                            nextInBlockchain, nextInPendingPool, nextPlanting);
                }
            }

            /**
             * Address for which we receive the details, lowercased.
             */
            @NonNull
            public final String address;

            /**
             * Is the address tracked by CherryPicker?
             * <p>
             * If yes, this will be a non-null instance
             * of {@link GetTrackedAddresses.TrackedAddressesRequestResultPayload.TrackedAddressInformation}
             * just like if we’ve called GetTrackedAddresses.
             * <p>
             * If the address is not tracked, this will contain <code>null</code>.
             */
            public final GetTrackedAddresses.TrackedAddressesRequestResultPayload.@Nullable TrackedAddressInformation trackedAddressInformation;

            /**
             * The details about nonces for this address.
             */
            @NonNull
            public final Nonces nonces;

            @JsonCreator
            public AddressDetails(
                    @NonNull String address,
                    GetTrackedAddresses.TrackedAddressesRequestResultPayload.@Nullable TrackedAddressInformation trackedAddressInformation,
                    @NonNull Nonces nonces
            ) {
                assert (address != null) && EthUtils.Addresses.isValidLowercasedAddress(address) : address;
                assert nonces != null;

                this.address = address;
                this.trackedAddressInformation = trackedAddressInformation;
                this.nonces = nonces;
            }

            @Override
            public String toString() {
                return String.format("GetAddressDetails.AddressDetailsRequestResultPayload.AddressDetails(%s, %s, %s)",
                        address, trackedAddressInformation, nonces);
            }
        }

        /**
         * Results: the detailed information about the requested address.
         */
        @NonNull
        public final AddressDetails details;

        /**
         * Constructor.
         */
        @JsonCreator
        public AddressDetailsRequestResultPayload(@NonNull AddressDetails details) {
            assert details != null;
            this.details = details;
        }

        @Override
        public String toString() {
            return String.format("GetAddressDetails.AddressDetailsRequestResultPayload(%s)",
                    details);
        }
    }

    public static class AddressDetailsRequestResultFailure extends SpecificFailurePayload {
    }

    public static class Response
            extends CherryGardenResponseWithPayload<AddressDetailsRequestResultPayload, AddressDetailsRequestResultFailure> {

        @JsonCreator
        private Response(@NonNull ResponsePayload payload) {
            super(payload);
        }

        public Response(@NonNull AddressDetailsRequestResultPayload payload) {
            this((ResponsePayload) payload);
        }

        public Response(@NonNull CommonFailurePayload commonFailure) {
            this((ResponsePayload) commonFailure);
        }

        public Response(@NonNull AddressDetailsRequestResultFailure specificFailure) {
            this((ResponsePayload) specificFailure);
        }

        @NonNull
        public static Response fromCommonFailure(@NonNull CommonFailurePayload commonFailure) {
            assert commonFailure != null : commonFailure;
            return new Response(commonFailure);
        }
    }
}
