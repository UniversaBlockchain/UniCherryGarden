package com.myodov.unicherrygarden.connector.api;

import com.myodov.unicherrygarden.connector.impl.Validators;
import com.myodov.unicherrygarden.messages.cherrygardener.GetCurrencies;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashSet;
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
     * Depending on the settings, may include verified, unverified currencies, or both;
     * also may select some currencies by their keys, or select all the currencies possible.
     * <p>
     * If you want just a single currency, use the similar {@link #getCurrency(String, boolean, boolean)}.
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
     * <p>
     * If you want just a single currency, use the similar {@link #getCurrency(String)}.
     *
     * @return the response with all the supported currencies.
     */
    @SuppressWarnings("unused")
    default GetCurrencies.@NonNull Response getCurrencies() {
        return getCurrencies(null, true, false);
    }

    /**
     * Get a single supported currency, by its key.
     * Depending on the settings, may include verified, unverified currencies, or both.
     * <p>
     * If you want multiple currencies at once, use the similar {@link #getCurrencies(Set, boolean, boolean)}.
     *
     * @param currencyKey a currency key
     * @return the response with just this currency.
     * @throws IllegalArgumentException if neither `getVerified` nor `getUnverified` are defined.
     */
    @SuppressWarnings("unused")
    default GetCurrencies.@NonNull Response getCurrency(
            @NonNull String currencyKey,
            boolean getVerified,
            boolean getUnverified) {
        Validators.requireValidCurrencyKey("currencyKey", currencyKey);
        return getCurrencies(
                new HashSet<String>(1) {{
                    add(currencyKey);
                }},
                getVerified,
                getUnverified
        );
    }

    /**
     * Get a single supported currency, by its key. The currency is returned only if it is pre-validated (“verified”).
     * <p>
     * If you want multiple currencies at once, use the similar {@link #getCurrencies()}.
     *
     * @return the response with all the supported currencies.
     */
    @SuppressWarnings("unused")
    default GetCurrencies.@NonNull Response getCurrency(@NonNull String currencyKey) {
        return getCurrency(currencyKey, true, false);
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
