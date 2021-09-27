package com.myodov.unicherrygarden.cherrygardener.connector.api;

import com.myodov.unicherrygarden.impl.types.dlt.CurrencyImpl;
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
    List<CurrencyImpl> getCurrencies();

    /** Stop whole connector to shut it down. */
    void shutdown();
}
