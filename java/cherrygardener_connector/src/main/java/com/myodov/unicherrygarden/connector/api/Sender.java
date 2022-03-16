package com.myodov.unicherrygarden.connector.api;

import com.myodov.unicherrygarden.api.types.planted.transactions.SignedOutgoingTransaction;
import com.myodov.unicherrygarden.api.types.planted.transactions.UnsignedOutgoingTransaction;
import com.myodov.unicherrygarden.messages.Serializable;
import com.myodov.unicherrygarden.messages.cherryplanter.PlantTransaction;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * The client connector part that ensures sending of the Ethereum ETH/ERC20 payments.
 */
public interface Sender {

    /**
     * How you want your nonce to be configured?
     */
    interface NonceSource {

        @SuppressWarnings("unused")
        enum Selector implements Serializable {
            BLOCKCHAIN,
            PENDING_POOL,
            CHERRYPLANTER
        }

        /**
         * Specify your nonce by a particular number.
         *
         * @param nonce number of nonce you want to use.
         *              Should be 0 <= nonce <= 2^64-1 per EIP-2681; uses `int` type as all existing data
         *              doesn't have any nonces even close to reach the limit of MAXINT.
         */
        @NonNull NonceSource fromNumber(int nonce);

        /**
         * Specify your nonce as next by some data stored in blockchain / pending pool / CherryPlanter.
         * Most of the times you want to use “next by CherryPlanter.”
         *
         * @param selector “next by what data” you want to use to select your Nonce.
         */
        @NonNull NonceSource nextBy(@NonNull Selector selector);
    }

    /**
     * Prepare outgoing transaction without signing it. User should somehow sign it and use it
     * in {@link #sendTransaction}.
     * The signing could be performed either with connector's provided signature function locally or by
     * remote part using some other signing software/backend we'll develop later.
     * <p>
     * This is the most-configurable implementation to be overridden; there are other more convenient ones to use,
     * like {@link #createOutgoingTransfer(String, String, String, BigDecimal).}
     *
     * @param sender              the address of the sender;
     *                            if present, should be lowercased Ethereum address
     *                            (will be used to autodetect the nonce).
     * @param receiver            the address of the receiver; should be lowercased Ethereum address.
     * @param currencyKey         the key of the currency to send.
     *                            Should be an empty string, if sending the primary currency of the blockchain
     *                            (ETH for Ethereum Mainnet, or maybe ETC in case if the backend is used
     *                            for other Ethereum-compatible forks).
     *                            Otherwise, it is a lowercased address of dApp token contract.
     * @param amount              amount to transfer.
     * @param forceDecimals       amount of decimals to use.
     *                            <code>18</code> for ETH/ERC/other primary currency (
     *                            i.e. if currencyKey is <code>""</code>) and may be omitted;
     *                            if omitted for ERC20 token, it is assumed the token is verified,
     *                            and the decimals value will be auto-detected.
     * @param forceChainId        Chain ID (EIP-155) to use.
     *                            If <code>null</code>, will be autodetected
     *                            (only if the sender is created in non-offline mode).
     * @param forceGasLimit       gas limit to use.
     *                            If <code>null</code>, will be autodetected
     *                            (only if the sender is created in non-offline mode).
     * @param forceNonce          force Nonce value to use.
     *                            If <code>null</code>, will be autodetected
     *                            (only if the sender is created in non-offline mode).
     * @param forceMaxPriorityFee force maxPriorityFee value (EIP-1559) to use.
     *                            If <code>null</code>, will be autodetected.
     * @param forceMaxFee         force maxFee value (EIP-1559) to use.
     *                            If <code>null</code>, will be autodetected.
     * @return the binary serialized transaction that user can sign on their side,
     * even using the external software (like MyCrypto/MyEtherWallet).
     * @apiNote happens directly in the memory space of the process, without ever leaving it.
     * No network communication is performed. Can be used in the connector launched in “offline” mode.
     */
    @NonNull
    UnsignedOutgoingTransaction createOutgoingTransfer(
            @Nullable String sender,
            @NonNull String receiver,
            @NonNull String currencyKey,
            @NonNull BigDecimal amount,
            @Nullable Integer forceDecimals,
            @Nullable Long forceChainId,
            @Nullable BigInteger forceGasLimit,
            @Nullable BigInteger forceNonce,
            @Nullable BigDecimal forceMaxPriorityFee,
            @Nullable BigDecimal forceMaxFee
    );

    /**
     * Prepare outgoing transaction without signing it. User should somehow sign it and use it
     * in {@link #sendTransaction}.
     * The signing could be performed either with connector's provided signature function locally or by
     * remote part using some other signing software/backend we'll develop later.
     * <p>
     * This is the simplest version of API:
     * <ul>
     * <li>decimals value will be detected for the currency (assuming the currency is verified);</li>
     * <li>Chain ID will be autodetected from the network (using the realm of the connector);</li>
     * <li>gas limit will be autodetected from the network for the specified currency;</li>
     * <li>nonce will be autodetected from the network (next unused nonce will be taken);</li>
     * <li>maxPriorityFee will be autodetected;</li>
     * <li>maxFee will be autodetected.</li>
     * </ul>
     *
     * @param sender      the address of the sender; should be lowercased Ethereum address
     *                    (will be used to autodetect the nonce).
     * @param receiver    the address of the receiver; should be lowercased Ethereum address.
     * @param currencyKey the key of the currency to send.
     *                    Should be an empty string, if sending the primary currency of the blockchain
     *                    (ETH for Ethereum Mainnet, or maybe ETC in case if the backend is used
     *                    for other Ethereum-compatible forks).
     *                    Otherwise, it is a lowercased address of dApp token contract.
     * @param amount      amount to transfer.
     * @return the binary serialized transaction that user can sign on their side,
     * even using the external software (like MyCrypto/MyEtherWallet).
     * @apiNote happens directly in the memory space of the process, without ever leaving it.
     * No network communication is performed. Can be used in the connector launched in “offline” mode.
     */
    @SuppressWarnings("unused")
    @NonNull
    default UnsignedOutgoingTransaction createOutgoingTransfer(
            @NonNull String sender,
            @NonNull String receiver,
            @NonNull String currencyKey,
            @NonNull BigDecimal amount
    ) {
        return createOutgoingTransfer(
                sender, receiver, currencyKey, amount, null,
                null, null, null, null, null);
    }

    /**
     * Sign the transaction.
     *
     * @apiNote happens directly in the memory space of the process, without ever leaving it.
     * No network communication is performed. Can be used in the connector launched in “offline” mode.
     */
    @SuppressWarnings("unused")
    @NonNull
    SignedOutgoingTransaction signTransaction(
            @NonNull UnsignedOutgoingTransaction tx,
            byte[] privateKey
    );

    /**
     * Enqueue the transaction for sending (try to send it to the blockchain, etc).
     *
     * @return the typical network-related response as a result of network communication with CherryPlanter.
     */
    @SuppressWarnings("unused")
    PlantTransaction.@NonNull Response sendTransaction(@NonNull SignedOutgoingTransaction tx);
}
