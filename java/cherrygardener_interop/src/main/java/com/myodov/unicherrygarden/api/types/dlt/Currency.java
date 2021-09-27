package com.myodov.unicherrygarden.api.types.dlt;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Any asset tracked by UniCherryGarden (cryptocurrency, token, etc).
 */
public interface Currency {
    enum CurrencyType {
        ETH,
        ERC20
    }

    /**
     * The only primary key to uniquely identify this currency.
     *
     * @implNote For Ethereum cryptocurrency, returns <code>""</code> (empty string).
     * For ERC20 tokens (or any other tokens), returns a string like
     * <code>"0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7"</code>
     * which is the address of underlying dApp.
     */
    @NonNull
    String getKey();

    /**
     * Get the currency type of this currency: is this a top-level blockchain “ETH” coin,
     * or some token, like ERC20 token.
     */
    @NonNull
    CurrencyType getCurrencyType();

    /**
     * Get currency/asset name, human readable but not necessarily unique, e.g. “Ethereum” or “Universa UTNP”.
     * Usually relatively short, but may contain multiple words.
     * For most legit currencies it’s unique, but there may be fake currencies that spoof existing ones.
     * It can even be missing (and is <code>null</code> in this case).
     */
    @Nullable
    String getName();

    /**
     * Get currency/asset “ticker symbol” (or “ticker code”), not necessarily unique.
     * Ticker code is a short several letters that are used to distinguish it on the exchanges.
     * For most legit currencies it’s unique, but there may be fake currencies that spoof existing ones.
     * It can even be missing (and is <code>null</code> in this case).
     */
    @Nullable
    String getSymbol();

    /**
     * Get the Ethereum address for the underlying dApp of this currency.
     * Returns <code>null</code> for Ethereum cryptocurrency.
     *
     * @implNote the address is lowercased, not in EIP55. Example return string:
     * <code>"0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7"</code>.
     */
    @Nullable
    String getDAppAddress();

    /**
     * Get a comment set at this currency by UniCherryGarden admins.
     * Optional, may return <code>null</code>.
     */
    @Nullable
    String getComment();
}
