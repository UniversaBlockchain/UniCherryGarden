package com.myodov.unicherrygarden.connector.api;

import com.myodov.unicherrygarden.messages.cherrypicker.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

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
     */
    AddTrackedAddresses.@NonNull Response startTrackingAddress(
            @NonNull String address,
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
     */
    default AddTrackedAddresses.@NonNull Response startTrackingAddressFromBlock(
            @NonNull String address,
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
     */
    default AddTrackedAddresses.@NonNull Response startTrackingAddress(
            @NonNull String address,
            AddTrackedAddresses.@NonNull StartTrackingAddressMode mode,
            @Nullable String comment) {
        assert mode != AddTrackedAddresses.StartTrackingAddressMode.FROM_BLOCK : mode;
        return startTrackingAddress(address, mode, null, comment);
    }

    /**
     * Get the list of addresses that are tracked by UniCherryGarden.
     * Each address is regular Ethereum address string, lowercased.
     */
    GetTrackedAddresses.@NonNull Response getTrackedAddresses();

    /**
     * Get the details about any Ethereum address, tracked or not.
     *
     * @param address the regular Ethereum address string, lowercased.
     *                For this address the details will be returned.
     */
    GetAddressDetails.@NonNull Response getAddressDetails(@NonNull String address);

    /**
     * Get the balances of some Ethereum address.
     *
     * @param address            The Ethereum address to get the balances for.
     * @param filterCurrencyKeys (optional) the set of the currency keys, for which to get the balances.
     *                           If <code>null</code>, gets the balances for all the supported currencies.
     *                           (Note if the set is empty, it will return the empty balances).
     * @param confirmations      The number of extra confirmations required, i.e. the offset from the latest data.
     *                           Should be 0 or higher. Normally it is 6–12 confirmations,
     *                           20 confirmations on large crypto exchanges.
     *                           Each confirmation roughly takes 15 seconds, i.e. 4 confirmations per minute.
     */
    GetBalances.@NonNull Response getAddressBalances(
            int confirmations,
            @NonNull String address,
            @Nullable Set<String> filterCurrencyKeys);

    /**
     * Get transfers (optionally filtered by currency, sender, receiver, start-block number, end-block number).
     * Optionally, the balances of mentioned addreses (whether `sender` or `receiver` are mentioned) are requested.
     *
     * @param confirmations      The number of extra confirmations required, i.e. the offset from the latest data.
     *                           Should be 0 or higher. Normally it is 6–12 confirmations,
     *                           20 confirmations on large crypto exchanges.
     *                           Each confirmation roughly takes 15 seconds, i.e. 4 confirmations per minute.
     * @param sender             (optional; at least one of <code>sender</code> or <code>receiver</code> is mandatory)
     *                           only transfers sent <b>from</b> this Ethereum address should be present;
     *                           must contain a valid lowercased Ethereum address.
     *                           If <code>null</code>, the results are not filtered to have the specific sender.
     * @param receiver           (optional; at least one of <code>sender</code> or <code>receiver</code> is mandatory)
     *                           only transfers sent <b>to</b> this Ethereum address should be present;
     *                           must contain a valid lowercased Ethereum address.
     *                           If <code>null</code>, the results are not filtered to have the specific receiver.
     * @param startBlock         (optional) the first block number (of Ethereum blockchain) containing
     *                           the transfers to find.
     *                           (Note: the transfers in this block <b>are</b> included).
     *                           If <code>null</code>, the transfers are returned from the earliest available block.
     * @param endBlock           (optional) the last block number (of Ethereum blockchain) containing
     *                           the transfers to find.
     *                           (Note: the transfers in this block <b>are</b> included).
     *                           If <code>null</code>, the transfers are returned till the latest available block.
     * @param filterCurrencyKeys (optional) the set of the currency keys, for which to get the balances.
     *                           If <code>null</code>, gets the balances for all the supported currencies.
     *                           (Note if the set is empty, it will return the empty balances).
     * @param includeBalances    Should the final balances be returned in the <code>transfers</code> part of the result.
     *                           If <code>false</code>, <code>result.transfers</code> will be empty.
     */
    GetTransfers.@NonNull Response getTransfers(
            int confirmations,
            @Nullable String sender,
            @Nullable String receiver,
            @Nullable Integer startBlock,
            @Nullable Integer endBlock,
            @Nullable Set<String> filterCurrencyKeys,
            boolean includeBalances);

    /**
     * Get transfers (optionally filtered by currency, sender, receiver, start-block number, end-block number).
     *
     * @param confirmations      The number of extra confirmations required, i.e. the offset from the latest data.
     *                           Should be 0 or higher. Normally it is 6–12 confirmations,
     *                           20 confirmations on large crypto exchanges.
     *                           Each confirmation roughly takes 15 seconds, i.e. 4 confirmations per minute.
     * @param sender             (optional; at least one of <code>sender</code> or <code>receiver</code> is mandatory)
     *                           only transfers sent <b>from</b> this Ethereum address should be present;
     *                           must contain a valid lowercased Ethereum address.
     *                           If <code>null</code>, the results are not filtered to have the specific sender.
     * @param receiver           (optional; at least one of <code>sender</code> or <code>receiver</code> is mandatory)
     *                           only transfers sent <b>to</b> this Ethereum address should be present;
     *                           must contain a valid lowercased Ethereum address.
     *                           If <code>null</code>, the results are not filtered to have the specific receiver.
     * @param startBlock         (optional) the first block number (of Ethereum blockchain) containing
     *                           the transfers to find.
     *                           (Note: the transfers in this block <b>are</b> included).
     *                           If <code>null</code>, the transfers are returned from the earliest available block.
     * @param endBlock           (optional) the last block number (of Ethereum blockchain) containing
     *                           the transfers to find.
     *                           (Note: the transfers in this block <b>are</b> included).
     *                           If <code>null</code>, the transfers are returned till the latest available block.
     * @param filterCurrencyKeys (optional) the set of the currency keys, for which to get the balances.
     *                           If <code>null</code>, gets the balances for all the supported currencies.
     *                           (Note if the set is empty, it will return the empty balances).
     * @return The structure containing the data about the balances;
     * or <code>null</code> if the request failed.
     * Note that the balances are <b>not</b> requested, so the result will contain
     * the empty <code>result.transfers</code> field.
     */
    default GetTransfers.@NonNull Response getTransfers(
            int confirmations,
            @Nullable String sender,
            @Nullable String receiver,
            @Nullable Integer startBlock,
            @Nullable Integer endBlock,
            @Nullable Set<String> filterCurrencyKeys) {
        return getTransfers(confirmations, sender, receiver, startBlock, endBlock, filterCurrencyKeys);
    }
}
