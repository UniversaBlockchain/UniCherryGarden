package com.myodov.unicherrygarden.messages.cherrypicker;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.ServiceKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.api.types.SystemSyncStatus;
import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.messages.CherryPickerRequest;
import com.myodov.unicherrygarden.messages.CherryPickerResponseWithResult;
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
         * The total status of blockchain synchronization.
         */
        @NonNull
        public final SystemSyncStatus syncStatus;

        @NonNull
        public final List<CurrencyBalanceFact> balances;

        /**
         * Constructor.
         */
        @JsonCreator
        public BalanceRequestResult(@NonNull SystemSyncStatus syncStatus,
                                    @NonNull List<CurrencyBalanceFact> balances) {
            assert syncStatus != null;
            assert balances != null;

            this.syncStatus = syncStatus;
            this.balances = Collections.unmodifiableList(balances);
        }

        @Override
        public String toString() {
            return String.format("BalanceRequestResult.BalanceRequestResult(%s, %s)",
                    syncStatus, balances);
        }
    }


    public static final class Response extends CherryPickerResponseWithResult<BalanceRequestResult> {
        @JsonCreator
        public Response(@Nullable BalanceRequestResult result) {
            super(result);
        }
    }
}
