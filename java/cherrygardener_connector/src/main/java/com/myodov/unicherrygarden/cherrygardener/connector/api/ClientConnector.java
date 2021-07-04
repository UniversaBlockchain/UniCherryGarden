package com.myodov.unicherrygarden.cherrygardener.connector.api;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Currency;
import java.util.List;

/**
 * The general interface for creating “cherry gardeners”, i.e. connectors
 * to UniCherryGarden that access all the functionality and features of Ethereum blockchain.
 */
public interface ClientConnector extends Keygen, AddressOwnershipConfirmator, Sender, Receiver {
    @NonNull
    List<Currency> getCurrencies();
}
