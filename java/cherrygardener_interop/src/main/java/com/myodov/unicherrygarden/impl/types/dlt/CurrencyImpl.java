package com.myodov.unicherrygarden.impl.types.dlt;

import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Default implementation of {@link Currency} interface.
 */
public class CurrencyImpl implements Currency {

    @NonNull
    protected final CurrencyType type;

    @Nullable
    protected final String dAppAddress;

    @Nullable
    protected final String name;

    @Nullable
    protected final String symbol;

    @Nullable
    protected final String comment;

    /**
     * Standard constructor.
     *
     * @param dAppAddress should be lowercased or <code>null</code>.
     */
    public CurrencyImpl(
            @NonNull CurrencyType type,
            @Nullable String dAppAddress,
            @Nullable String name,
            @Nullable String symbol,
            @Nullable String comment
    ) {
        // Validate dAppAddress
        switch (type) {
            // For Ethereum, dAppAddress should be empty
            case ETH:
                assert dAppAddress == null : dAppAddress;
                break;
            // For ERC20 tokens, dAppAddress should be lowercased valid Ethereum address
            case ERC20:
                assert dAppAddress.toLowerCase().equals(dAppAddress) : dAppAddress;
                assert EthUtils.Addresses.isValidAddress(dAppAddress) : dAppAddress;
                break;
            default:
                assert false : type;
        }

        // Validate name and symbols
        if (type == CurrencyType.ETH) {
            assert name.equals("Ether") : name;
            assert symbol.equals("ETH") : symbol;
        }

        this.type = type;
        this.dAppAddress = dAppAddress;
        this.name = name;
        this.symbol = symbol;
        this.comment = comment;
    }

    /**
     * Create a new "ETH" currency.
     */
    public static final CurrencyImpl newEthCurrency() {
        return new CurrencyImpl(
                CurrencyType.ETH,
                null,
                "Ether",
                "ETH",
                null);
    }

    /**
     * Create a new "ERC20" token.
     */
    public static final CurrencyImpl newErc20Token(
            @NonNull String dAppAddress,
            @Nullable String name,
            @Nullable String symbol,
            @Nullable String comment
    ) {
        assert dAppAddress != null && EthUtils.Addresses.isValidAddress(dAppAddress) : dAppAddress;

        return new CurrencyImpl(CurrencyType.ERC20, dAppAddress, name, symbol, comment);
    }

    @Override
    public String toString() {
        return String.format("CurrencyImpl(%s, %s, %s, %s)",
                type, symbol, name, dAppAddress);
    }


    @Override
    @NonNull
    public String getKey() {
        switch (type) {
            case ETH:
                return "";
            case ERC20:
                return dAppAddress;
            default:
                throw new RuntimeException(String.format("Unsupported currency type %s", type));
        }
    }

    @Override
    @NonNull
    public CurrencyType getCurrencyType() {
        return type;
    }

    @Override
    @Nullable
    public String getName() {
        return name;
    }

    @Override
    @Nullable
    public String getSymbol() {
        return symbol;
    }

    @Override
    @Nullable
    public String getDAppAddress() {
        switch (type) {
            case ETH:
                return null;
            // For ERC20 tokens, dAppAddress should be lowercased valid Ethereum address
            case ERC20:
                return dAppAddress;
            default:
                throw new RuntimeException(String.format("Unsupported currency type %s", type));
        }
    }

    @Override
    @Nullable
    public String getComment() {
        return comment;
    }
}
