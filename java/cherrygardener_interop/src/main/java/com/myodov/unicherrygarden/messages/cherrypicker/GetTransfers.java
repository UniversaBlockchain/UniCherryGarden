package com.myodov.unicherrygarden.messages.cherrypicker;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.ServiceKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.api.types.BlockchainSyncStatus;
import com.myodov.unicherrygarden.api.types.MinedTransfer;
import com.myodov.unicherrygarden.messages.CherryPickerRequest;
import com.myodov.unicherrygarden.messages.CherryPickerResponse;
import com.myodov.unicherrygarden.messages.RequestPayload;
import com.myodov.unicherrygarden.messages.RequestWithReplyTo;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collections;
import java.util.List;


public class GetTransfers {
    @NonNull
    public static final ServiceKey<GetBalances.Request> SERVICE_KEY =
            ServiceKey.create(GetBalances.Request.class, "getTransfersService");

    public static final class GTRequestPayload
            implements RequestPayload {

    }

    public static final class Request
            extends RequestWithReplyTo<GetTransfers.GTRequestPayload, GetTransfers.Response>
            implements CherryPickerRequest {
        @JsonCreator
        public Request(@NonNull ActorRef<GetTransfers.Response> replyTo,
                       @NonNull GTRequestPayload payload) {
            super(replyTo, payload);
        }
    }


    public static class TransfersRequestResult {
        /**
         * Whether or not the transfers (stored in {@link #transfers}) have been retrieved,
         * at least partially, at least non-fully-synced. “Overall success” of getting the list of transfers.
         * <p>
         * If {@link #overallSuccess} is <code>false</code>, we should assume the result retrieval
         * failed completely, and {@link #transfers} will likely have no records at all.
         * <p>
         * There also may be some partial-fails (like, only the transfers for some specific token failed);
         * in this case, the balance will have {@link TransfersRequestResult::TransfersSyncState#NON_SYNCED} state.
         */
        public final boolean overallSuccess;

        /**
         * The number of the block for which the results have been provided.
         */
        public final int resultAtBlock;

        @NonNull
        public final List<MinedTransfer> transfers;

        /**
         * The total status of blockchain synchronization.
         */
        @NonNull
        public final BlockchainSyncStatus syncStatus;


        /**
         * Constructor.
         */
        @JsonCreator
        public TransfersRequestResult(boolean overallSuccess,
                                      int resultAtBlock,
                                      @NonNull List<MinedTransfer> transfers,
                                      @NonNull BlockchainSyncStatus syncStatus) {
            assert transfers != null;
            assert syncStatus != null;

            this.overallSuccess = overallSuccess;
            this.resultAtBlock = resultAtBlock;
            this.transfers = Collections.unmodifiableList(transfers);

            this.syncStatus = syncStatus;
        }
    }


    public static final class Response
            implements CherryPickerResponse {
        @NonNull
        public final TransfersRequestResult result;

        @JsonCreator
        public Response(@NonNull TransfersRequestResult result) {
            this.result = result;
        }

        @Override
        public String toString() {
            return String.format("GetTransfers.Response(%s)", result);
        }
    }
}
