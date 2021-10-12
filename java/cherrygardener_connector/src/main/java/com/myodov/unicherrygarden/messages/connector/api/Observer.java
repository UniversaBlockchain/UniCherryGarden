package com.myodov.unicherrygarden.messages.connector.api;

import com.myodov.unicherrygarden.messages.cherrypicker.AddTrackedAddresses;
import com.myodov.unicherrygarden.messages.cherrypicker.GetBalances;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
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
     *                    Must be a properly formed Ethereum address string,
     *                    like "0x34e1e4f805fcdc936068a760b2c17bc62135b5ae".
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
     *                Must be a properly formed Ethereum address string,
     *                like "0x34e1e4f805fcdc936068a760b2c17bc62135b5ae".
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
     */
    @Nullable
    List<@NonNull String> getTrackedAddresses();


    /**
     * Get the balances of some Ethereum address.
     *
     * @param address            The Ethereum address to get the balances for.
     * @param filterCurrencyKeys (optional) the set of the currency keys, for which to get the balances.
     *                           If <code>null</code>, gets the balances for all the supported currencies.
     *                           (Note if the set is empty, it will return the empty balances).
     * @param confirmations      The number of confirmations required, i.e. the offset from the latest data.
     *                           Should be 0 or higher. Normally it is 6–12 confirmations,
     *                           20 confirmations on large crypto exchanges.
     *                           Each confirmation roughly takes 15 seconds, i.e. 4 confirmations per minute.
     * @return The structure containing the data about the balances.
     * Check the {@link GetBalances.BalanceRequestResult#overallSuccess} to be sure the data inside is legit.
     */
    GetBalances.@NonNull BalanceRequestResult getAddressBalances(@NonNull String address,
                                                                 @Nullable Set<String> filterCurrencyKeys,
                                                                 int confirmations);
}
