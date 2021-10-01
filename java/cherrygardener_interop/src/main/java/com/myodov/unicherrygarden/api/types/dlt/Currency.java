package com.myodov.unicherrygarden.api.types.dlt;

import com.myodov.unicherrygarden.ethereum.EthUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

/**
 * Any asset tracked by UniCherryGarden (cryptocurrency, token, etc).
 *
 * @implNote Concrete class instead of some Java interface,
 * so it can be directly used in Akka serialized messages.
 */
public class Currency {
    public enum CurrencyType {
        ETH,
        ERC20
    }

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
    public Currency(
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
                assert EthUtils.Addresses.isValidLowercasedAddress(dAppAddress) : dAppAddress;
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
    public static final Currency newEthCurrency() {
        return new Currency(
                CurrencyType.ETH,
                null,
                "Ether",
                "ETH",
                null);
    }

    /**
     * Create a new "ERC20" token.
     */
    public static final Currency newErc20Token(
            @NonNull String dAppAddress,
            @Nullable String name,
            @Nullable String symbol,
            @Nullable String comment
    ) {
        assert dAppAddress != null && EthUtils.Addresses.isValidLowercasedAddress(dAppAddress) : dAppAddress;

        return new Currency(CurrencyType.ERC20, dAppAddress, name, symbol, comment);
    }

    @Override
    public String toString() {
        return String.format("CurrencyImpl(%s, %s, %s, %s)",
                type, symbol, name, dAppAddress);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof Currency)) {
            return false;
        } else {
            final Currency other = (Currency) o;
            return (this.type == other.type) && (this.dAppAddress == other.dAppAddress);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.dAppAddress);
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

    /**
     * Get the currency type of this currency: is this a top-level blockchain “ETH” coin,
     * or some token, like ERC20 token.
     */
    @NonNull
    public CurrencyType getCurrencyType() {
        return type;
    }

    /**
     * Get currency/asset name, human readable but not necessarily unique, e.g. “Ethereum” or “Universa UTNP”.
     * Usually relatively short, but may contain multiple words.
     * For most legit currencies it’s unique, but there may be fake currencies that spoof existing ones.
     * It can even be missing (and is <code>null</code> in this case).
     */
    @Nullable
    public String getName() {
        return name;
    }

    /**
     * Get currency/asset “ticker symbol” (or “ticker code”), not necessarily unique.
     * Ticker code is a short several letters that are used to distinguish it on the exchanges.
     * For most legit currencies it’s unique, but there may be fake currencies that spoof existing ones.
     * It can even be missing (and is <code>null</code> in this case).
     */
    @Nullable
    public String getSymbol() {
        return symbol;
    }

    /**
     * Get the Ethereum address for the underlying dApp of this currency.
     * Returns <code>null</code> for Ethereum cryptocurrency.
     *
     * @implNote the address is lowercased, not in EIP55. Example return string:
     * <code>"0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7"</code>.
     */
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

    /**
     * Get a comment set at this currency by UniCherryGarden admins.
     * Optional, may return <code>null</code>.
     */
    @Nullable
    public String getComment() {
        return comment;
    }
}
