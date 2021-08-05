package com.myodov.unicherrygarden.cherrygardener.connector.api;

import com.myodov.unicherrygarden.cherrygardener.connector.api.types.Currency;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

/**
 * The general interface for creating “cherry gardeners”, i.e. connectors
 * to UniCherryGarden that access all the functionality and features of Ethereum blockchain.
 */
public interface ClientConnector extends AddressOwnershipConfirmator
        // extends Keygen, Sender, Receiver
{
    @NonNull
    List<Currency> getCurrencies();

    /** Stop whole connector to shut it down. */
    void shutdown();
}
