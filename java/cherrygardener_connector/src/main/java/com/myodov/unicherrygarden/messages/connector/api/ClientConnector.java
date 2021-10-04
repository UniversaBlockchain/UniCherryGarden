package com.myodov.unicherrygarden.messages.connector.api;

import com.myodov.unicherrygarden.api.types.dlt.Currency;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

/**
 * The general interface for creating “cherry gardeners”, i.e. connectors
 * to UniCherryGarden that access all the functionality and features of Ethereum blockchain.
 */
public interface ClientConnector
        // extends AddressOwnershipConfirmator, Keygen, Sender, Observer
{
    /**
     * Get all the supported currencies.
     *
     * @return <code>null</code> if any error occurred during getting the data
     * (in particular, if the client is in “offline mode”).
     * or the list of all supported currencies.
     */
    @Nullable
    List<Currency> getCurrencies();


    /**
     * Returns the engine/subsystem that allows you to confirm the Ethereum address ownership.
     */
    @NonNull
    AddressOwnershipConfirmator getAddressOwnershipConfirmator();

    /**
     * Returns the engine/subsystem that allows you to create valid Ethereum private keys.
     */
    @NonNull
    Keygen getKeygen();

    /**
     * Returns the engine/subsystem that allows you to observe the Ethereum addresses for all related transactions;
     * i.e. check the outbound transfers, inbound transfers and balances.
     * Basically it is the access to “CherryPicker” subsystem.
     *
     * @return <code>null</code> if the client connector is created in “offline mode”.
     */
    @Nullable
    Observer getObserver();

    /**
     * Stop whole connector to shut it down.
     */
    void shutdown();
}
