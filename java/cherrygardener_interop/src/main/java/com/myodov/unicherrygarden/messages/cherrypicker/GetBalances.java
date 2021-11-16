package com.myodov.unicherrygarden.messages.cherrypicker;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.ServiceKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.api.types.BlockchainSyncStatus;
import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.messages.CherryPickerRequest;
import com.myodov.unicherrygarden.messages.CherryPickerResponse;
import com.myodov.unicherrygarden.messages.RequestPayload;
import com.myodov.unicherrygarden.messages.RequestWithReplyTo;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;


public class GetBalances {
    @NonNull
    public static final ServiceKey<Request> SERVICE_KEY =
            ServiceKey.create(Request.class, "getBalancesService");


    public static final class GBRequestPayload
            implements RequestPayload {
        @Nullable
        public final Set<String> filterCurrencyKeys;

        @NonNull
        public final int confirmations;

        @JsonCreator
        public GBRequestPayload(int confirmations,
                                @Nullable Set<String> filterCurrencyKeys) {
            assert confirmations >= 0 : confirmations;

            this.filterCurrencyKeys = filterCurrencyKeys;
            this.confirmations = confirmations;
        }

        @Override
        public String toString() {
            return String.format("GetBalances.GBRequestPayload(%s, %s)",
                    filterCurrencyKeys, confirmations);
        }
    }


    public static final class Request
            extends RequestWithReplyTo<GBRequestPayload, Response>
            implements CherryPickerRequest {
        @JsonCreator
        public Request(@NonNull ActorRef<Response> replyTo,
                       @NonNull GBRequestPayload payload) {
            super(replyTo, payload);
        }
    }


    public static class BalanceRequestResult {

        public static class CurrencyBalanceFact {
            /**
             * The state how well is balance (of some currency for some address) synced; i.e. how actual is the data.
             * The sync states are ordered; each next state is definitely better (more actual) than previous one.
             */
            public enum BalanceSyncState {
                /**
                 * The balance state is not fully synced to any known point and cannot be fully trusted.
                 */
                NON_SYNCED,
                /**
                 * The balance of this token at this address is synced at least to the latest state
                 * of UniCherryGarden sync for this token in general.
                 * <p>
                 * The sync state of this particular token may still lag behind the overall UniCherryGarden sync state.
                 */
                SYNCED_TO_LATEST_UNICHERRYGARDEN_TOKEN_STATE,
                /**
                 * The balance of this token at this address is synced at least to the latest state
                 * of UniCherryGarden global.
                 * Implies {@link #SYNCED_TO_LATEST_UNICHERRYGARDEN_TOKEN_STATE}.
                 * <p>
                 * This means that the address is synced to the latest overall UniCherryGarden sync state;
                 * but overall UniCherryGarden sync state may still lag behind the latest block available
                 * to the Ethereum node.
                 * <p>
                 * The global UniCherryGarden sync state can momentarily lag behind the latest node blocks,
                 * that is okay.
                 */
                SYNCED_TO_LATEST_UNICHERRYGARDEN_GLOBAL_STATE,
                /**
                 * The balance of this token at this address is synced at least to the latest block
                 * available to the Ethereum node.
                 * Implies {@link #SYNCED_TO_LATEST_UNICHERRYGARDEN_GLOBAL_STATE}.
                 * <p>
                 * This means that the address is synced to the latest blockchain data stored on the node;
                 * but it is possible the Ethereum node itself is still not fully synced to the latest block
                 * known to it. Though if the “latest synced block” lags behind the “latest known block”,
                 * like, a block or two, that is okay.
                 */
                SYNCED_TO_LATEST_BLOCKCHAIN_SYNC_STATE,
                /**
                 * The balance of this token at this address is synced to the latest block known to the Ethereum node.
                 * Implies {@link #SYNCED_TO_LATEST_BLOCKCHAIN_SYNC_STATE}.
                 * <p>
                 * This is the most ideal condition, when synced to everything; but momentary, it may be off.
                 * <p>
                 * This state means that the address is synced to the latest blockchain data stored on the node;
                 * but it is possible the Ethereum node itself is still not fully synced to the latest block
                 * known to it. Though if the “latest synced block” lags behind the “latest known block”,
                 * like, a block or two, that is okay.
                 */
                SYNCED_TO_LATEST_BLOCKCHAIN_KNOWN_STATE;
            }

            /**
             * The currency for which the balance is retrieved.
             */
            @NonNull
            public final Currency currency;

            /**
             * The amount of the balance.
             * Always non-null; always non-negative.
             * Even if some error occurred, as a safe default it will contain <code>0</code>.
             */
            @NonNull
            public final BigDecimal amount;

            /**
             * “How up-to-date” the balance information is.
             */
            public final BalanceSyncState syncState;

            /**
             * The information is up-to-date to this block number.
             * <p>
             * If present, always 0 or higher.
             */
            @Nullable
            public final Integer syncedToBlock;


            @JsonCreator
            public CurrencyBalanceFact(@NonNull Currency currency,
                                       @NonNull BigDecimal amount,
                                       @NonNull BalanceSyncState syncState,
                                       @Nullable Integer syncedToBlock) {
                assert currency != null;
                assert amount.compareTo(BigDecimal.ZERO) >= 0 : amount; // amount >= 0
                assert syncState != null;
                assert syncedToBlock == null || syncedToBlock.intValue() >= 0 : syncedToBlock;

                this.currency = currency;
                this.amount = amount;
                this.syncState = syncState;
                this.syncedToBlock = syncedToBlock;
            }

            @Override
            public String toString() {
                return String.format("BalanceRequestResult.CurrencyBalanceFact(%s: amount=%s: %s, synced to %s)",
                        currency, amount, syncState, syncedToBlock);
            }
        }

        /**
         * Whether or not the balances (stored in {@link #balances}) have been retrieved,
         * at least partially, at least non-fully-synced. “Overall success” of getting balances.
         * <p>
         * If {@link #overallSuccess} is <code>false</code>, we should assume the result retrieval
         * failed completely, and {@link #balances} will likely have no records at all.
         * But if it’s <code>true</code>, we still should doublecheck every {@link CurrencyBalanceFact}
         * if it’s up-to-date (as we need).
         * <p>
         * There also may be some partial-fails (like, only the balance for some specific token failed);
         * in this case, the balance will have {@link BalanceRequestResult::BalanceSyncState#NON_SYNCED} state.
         */
        public final boolean overallSuccess;

        /** The number of the block for which the results have been provided. */
        public final int resultAtBlock;

        @NonNull
        public final List<CurrencyBalanceFact> balances;

        /** The total status of blockchain synchronization. */
        @NonNull
        public final BlockchainSyncStatus syncStatus;


        /**
         * Constructor.
         */
        @JsonCreator
        public BalanceRequestResult(boolean overallSuccess,
                                    int resultAtBlock,
                                    @NonNull List<CurrencyBalanceFact> balances,
                                    @NonNull BlockchainSyncStatus syncStatus) {
            assert balances != null;
            assert syncStatus != null;

            this.overallSuccess = overallSuccess;
            this.resultAtBlock = resultAtBlock;

            this.balances = Collections.unmodifiableList(balances);

            this.syncStatus = syncStatus;
        }

        /**
         * Auxilary constructor: create an “Unsuccessful” result of getting balances.
         */
        public static final BalanceRequestResult unsuccessful() {
            return new BalanceRequestResult(
                    false,
                    0,
                    Collections.emptyList(),
                    new BlockchainSyncStatus(0, 0, 0));
        }

        @Override
        public String toString() {
            return String.format("BalanceRequestResult.BalanceRequestResult(success=%s, atBlock=%s, balances=%s, %s)",
                    overallSuccess,
                    resultAtBlock,
                    balances,
                    syncStatus);
        }
    }


    public static final class Response
            implements CherryPickerResponse {
        @NonNull
        public final BalanceRequestResult result;

        @JsonCreator
        public Response(@NonNull BalanceRequestResult result) {
            this.result = result;
        }

        @Override
        public String toString() {
            return String.format("GetBalances.Response(%s)", result);
        }
    }
}
