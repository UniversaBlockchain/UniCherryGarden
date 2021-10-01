package com.myodov.unicherrygarden.messages.connector.api;

import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.messages.cherrypicker.AddTrackedAddresses;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The client connector part that observes the Ethereum ETH/ERC20 payments.
 */
public interface Observer {

    /**
     * Start tracking an Ethereum address, to be overridden in any implementations.
     * Call the {@link #startTrackingAddressFromBlock(String, int, String)}
     * or {@link #startTrackingAddress(String, AddTrackedAddresses.StartTrackingAddressMode, String)} for your purposes.
     *
     * @param address     Ethereum address to track; should be lowercased.
     * @param blockNumber the block number from which to track the address.
     *                    Should be non-<code>null</code> if mode is <code>FROM_BLOCK</code>,
     *                    should be <code>null</code> otherwise.
     * @param comment     (Optional) comment to the tracked address; may freely be <code>null</code>
     *                    (which is different from <code>""</code>).
     * @return Whether the address is successfully added. If <code>False</code>, the attempt should probably be retried.
     */
    boolean startTrackingAddress(@NonNull String address,
                                 AddTrackedAddresses.@NonNull StartTrackingAddressMode mode,
                                 @Nullable Integer blockNumber,
                                 @Nullable String comment);


    /**
     * Start tracking an Ethereum address from a specific block number.
     *
     * @param address     Ethereum address to track; should be lowercased.
     * @param blockNumber the block number from which to track the address.
     * @param comment     (Optional) comment to the tracked address; may freely be <code>null</code>
     *                    (which is different from <code>""</code>).
     * @return Whether the address is successfully added. If <code>False</code>, the attempt should probably be retried.
     */
    default boolean startTrackingAddressFromBlock(@NonNull String address,
                                                  int blockNumber,
                                                  @Nullable String comment) {
        assert blockNumber >= 0 : blockNumber;
        return startTrackingAddress(
                address,
                AddTrackedAddresses.StartTrackingAddressMode.FROM_BLOCK,
                blockNumber,
                comment);
    }


    /**
     * Start tracking an Ethereum address, if you don’t know the specific block number.
     *
     * @param address Ethereum address to track; should be lowercased.
     * @param mode    how to find the block from which to track.
     *                You should pass any modes except {@link AddTrackedAddresses.StartTrackingAddressMode#FROM_BLOCK}
     *                here; use {@link #startTrackingAddressFromBlock(String, int, String)}
     *                if you want to specific a block explicitly.
     * @param comment (Optional) comment to the tracked address; may freely be <code>null</code>
     *                (which is different from <code>""</code>).
     * @return Whether the address is successfully added. If <code>False</code>, the attempt should probably be retried.
     */
    default boolean startTrackingAddress(@NonNull String address,
                                         AddTrackedAddresses.@NonNull StartTrackingAddressMode mode,
                                         @Nullable String comment) {
        assert mode != AddTrackedAddresses.StartTrackingAddressMode.FROM_BLOCK : mode;
        return startTrackingAddress(address, mode, null, comment);
    }


    /**
     * Get the list of addresses that are tracked by UniCherryGarden.
     * Each address is regular Ethereum address string, lowercased.
     *
     * @return <code>null</code> if any error occurred during getting the addresses.
     * or the list of Ethereum addresses tracked by UniCherryGarden.
     */
    @Nullable
    List<@NonNull String> getTrackedAddresses();


    class BalanceRequestResult {

        static class CurrencyBalanceFact {
            /**
             * The state how well is balance (of some currency for some address) synced; i.e. how actual is the data.
             * The sync states are ordered; each next state is definitely better (more actual) than previous one.
             */
            enum BalanceSyncState {
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
             * The amount of the balance.
             * Always non-null; always non-negative.
             * Even if some error occurred, as a safe default it will contain <code>0</code>.
             */
            @NonNull
            public final BigDecimal amount;

            /**
             * “How up-to-date” the balance information is.
             */
            @NonNull
            public final BalanceSyncState syncState;

            /**
             * The information is up-to-date to this block number.
             * <p>
             * Always 0 or higher.
             */
            public final int syncedToBlock;


            CurrencyBalanceFact(@NonNull BigDecimal amount,
                                @NonNull BalanceSyncState syncState,
                                int syncedToBlock) {
                assert amount.compareTo(BigDecimal.ZERO) >= 0 : amount; // amount >= 0
                assert syncedToBlock >= 0 : syncedToBlock;

                this.amount = amount;
                this.syncState = syncState;
                this.syncedToBlock = syncedToBlock;
            }

            @Override
            public String toString() {
                return String.format("CurrencyBalanceFact(amount=%s: %, synced to %s)",
                        amount, syncState, syncedToBlock);
            }
        }

        /**
         * Whether or not the balances (stored in {@link #balances}) have been retrieved,
         * at least partially, at least non-fully-synced.
         * <p>
         * If {@link #overallBalancesSuccess} is <code>false</code>, we should assume the balances retrieval
         * failed completely, and {@link #balances} will likely have no records at all.
         * But if it’s <code>true</code>, we still should doublecheck every {@link CurrencyBalanceFact}
         * if it’s up-to-date (as we need).
         * <p>
         * There also may be some partial-fails (like, only the balance for some specific token failed);
         * in this case, the balance will have {@link BalanceRequestResult::BalanceSyncState#NON_SYNCED} state.
         */
        public boolean overallBalancesSuccess;

        @NonNull
        public Map<Currency, CurrencyBalanceFact> balances;

        /**
         * The number of the latest block known to the Ethereum node.
         */
        public int latestBlockchainKnownBlock;

        /**
         * The number of the latest block the Ethereum node is synced to.
         * <p>
         * Normally <code>{@link #latestBlockchainSyncedBlock} <= {@link #latestBlockchainKnownBlock}</code>.
         * <p>
         * In ideal sync state, <code>{@link #latestBlockchainSyncedBlock} = {@link #latestBlockchainKnownBlock}</code>;
         * but if it momentarily lags 1 or 2 blocks behind {@link #latestBlockchainKnownBlock}, it is also okay.
         */
        public int latestBlockchainSyncedBlock;

        /**
         * The number of the latest block the UniCherryGarden considers itself globally synced to.
         * <p>
         * Can momentarily lag some blocks behind {@link #latestBlockchainSyncedBlock}
         * or {@link #latestBlockchainKnownBlock}.
         */
        public int latestUniCherryGardenSyncedBlock;
    }

    /**
     * Get the balances of some Ethereum address.
     *
     * @param address            The Ethereum address to get the balances for.
     * @param filterCurrencyKeys (optional) the set of the currency keys, for which to get the balances.
     *                           If <code>null</code>, gets the balances for all the supported currencies.
     *                           (Note if the set is empty, it will return the empty balances).
     */
    @NonNull
    BalanceRequestResult getAddressBalance(@NonNull String address,
                                           @Nullable Set<String> filterCurrencyKeys);
}
