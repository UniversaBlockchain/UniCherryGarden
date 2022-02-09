package com.myodov.unicherrygarden.connector.api;

import com.myodov.unicherrygarden.messages.cherrygardener.GetCurrencies;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;

/**
 * The general interface for creating “cherry gardeners”, i.e. connectors
 * to UniCherryGarden that access all the functionality and features of Ethereum blockchain.
 */
public interface ClientConnector {
    /**
     * Stop whole connector to shut it down.
     */
    @SuppressWarnings("unused")
    void shutdown();

    /**
     * Get all the supported currencies, or some subset of them.
     * Depending on the settings, may include verified, unverified currencies, or both; also may select some currencies
     * by their keys, or not
     *
     * @param filterCurrencyKeys if present, contains the currency keys that we are looking for;
     *                           if absent, all currencies should be retrieved.
     * @return the response with all the requested currencies.
     * @throws IllegalArgumentException if neither `getVerified` nor `getUnverified` are defined.
     */
    @SuppressWarnings("unused")
    GetCurrencies.@NonNull Response getCurrencies(
            @Nullable Set<String> filterCurrencyKeys,
            boolean getVerified,
            boolean getUnverified);

    /**
     * Get all the supported currencies. Only the currencies pre-validated (“verified”) are returned.
     *
     * @return the response with all the supported currencies.
     * @throws IllegalArgumentException if neither `getVerified` nor `getUnverified` are defined.
     */
    @SuppressWarnings("unused")
    default GetCurrencies.@NonNull Response getCurrencies() {
        return getCurrencies(null, true, false);
    }

    /**
     * Returns the engine/subsystem that allows you to confirm the Ethereum address ownership.
     */
    @NonNull
    @SuppressWarnings("unused")
    AddressOwnershipConfirmator getAddressOwnershipConfirmator();

    /**
     * Returns the engine/subsystem that allows you to create valid Ethereum private keys.
     */
    @NonNull
    @SuppressWarnings("unused")
    Keygen getKeygen();

    /**
     * Returns the engine/subsystem that allows you to observe the Ethereum addresses for all related transactions;
     * i.e. check the outbound transfers, inbound transfers and balances.
     * Basically it is the access to “CherryPicker” subsystem.
     *
     * @return <code>null</code> if the client connector is created in “offline mode”.
     */
    @Nullable
    @SuppressWarnings("unused")
    Observer getObserver();
}
