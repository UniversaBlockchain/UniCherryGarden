package com.myodov.unicherrygarden.api.types.dlt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import com.myodov.unicherrygarden.messages.Serializable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigInteger;
import java.util.Objects;

import static com.myodov.unicherrygarden.ethereum.EthUtils.ETH_TRANSFER_GAS_LIMIT_BIGINTEGER;

/**
 * Any asset tracked by UniCherryGarden (cryptocurrency, token, etc).
 *
 * @implNote Concrete class instead of some Java interface,
 * so it can be directly used in Akka serialized messages.
 */
public final class Currency implements Serializable {

    public enum CurrencyType implements Serializable {
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

    protected final boolean verified;

    @Nullable
    protected final Integer decimals;

    @Nullable
    protected final BigInteger transferGasLimit;


    /**
     * Standard constructor.
     *
     * @param dAppAddress should be lowercased or <code>null</code>.
     */
    @JsonCreator
    public Currency(
            @NonNull CurrencyType type,
            @Nullable String dAppAddress,
            @Nullable String name,
            @Nullable String symbol,
            @Nullable String comment,
            boolean verified,
            @Nullable Integer decimals,
            @Nullable BigInteger transferGasLimit
    ) {
        // Validate dAppAddress
        switch (type) {
            // For Ether, dAppAddress should be empty
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

        // Validate `verified` and `decimals`/`transferGasLimit`.
        switch (type) {
            // For Ether, decimals is undefined
            case ETH:
                assert decimals == null : decimals;
                break;
            // For ERC20 tokens, decimals should be defined (and between 0 and 79 inclusive)
            // if the token is verified
            case ERC20:
                assert !(verified && decimals == null) : String.format("%s/%s", verified, decimals);
                assert !(verified && transferGasLimit == null) : String.format("%s/%s", verified, transferGasLimit);
                break;
            default:
                assert false : type;
        }

        // Validate `transferGasLimit`
        switch (type) {
            // For Ether, transferGasLimit is 21 000
            case ETH:
                assert transferGasLimit != null && transferGasLimit.equals(ETH_TRANSFER_GAS_LIMIT_BIGINTEGER) : transferGasLimit;
                break;
            // For ERC20 tokens, transferGasLimit should be > 21000
            // if the token is verified
            case ERC20:
                // transferGasLimit > ETH_TRANSFER_GAS_LIMIT_BIGINTEGER
                assert transferGasLimit == null || transferGasLimit.compareTo(ETH_TRANSFER_GAS_LIMIT_BIGINTEGER) > 0 : transferGasLimit;
                break;
            default:
                assert false : type;
        }

        this.type = type;
        this.dAppAddress = dAppAddress;
        this.name = name;
        this.symbol = symbol;
        this.comment = comment;
        this.verified = verified;
        this.decimals = decimals;
        this.transferGasLimit = transferGasLimit;
    }

    /**
     * Create a new "ETH" currency.
     */
    public static Currency newEthCurrency() {
        return new Currency(
                CurrencyType.ETH,
                null,
                "Ether",
                "ETH",
                null,
                true,
                null,
                ETH_TRANSFER_GAS_LIMIT_BIGINTEGER);
    }

    /**
     * Create a new "ERC20" token.
     */
    public static Currency newErc20Token(
            @NonNull String dAppAddress,
            @Nullable String name,
            @Nullable String symbol,
            @Nullable String comment,
            boolean verified,
            @Nullable Integer decimals,
            @NonNull BigInteger transferGasLimit
    ) {
        assert dAppAddress != null && EthUtils.Addresses.isValidLowercasedAddress(dAppAddress) : dAppAddress;

        return new Currency(
                CurrencyType.ERC20,
                dAppAddress,
                name,
                symbol,
                comment,
                verified,
                decimals,
                transferGasLimit
        );
    }

    @Override
    public String toString() {
        return String.format("CurrencyImpl(%s, %s, %s, %s, verified=%s, decimals=%s, transferGasLimit=%s)",
                type, symbol, name, dAppAddress, verified, decimals, transferGasLimit);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            final Currency other = (Currency) o;
            return this.type == other.type &&
                    Objects.equals(this.symbol, other.symbol) &&
                    Objects.equals(this.dAppAddress, other.dAppAddress) &&
                    this.verified == other.verified &&
                    Objects.equals(this.decimals, other.decimals) &&
                    Objects.equals(this.transferGasLimit, other.transferGasLimit);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.dAppAddress);
    }

    /**
     * The only primary key (also called “currency key”) to uniquely identify this currency.
     *
     * @implNote For Ethereum cryptocurrency, returns <code>""</code> (empty string).
     * For ERC20 tokens (or any other tokens), returns a string like
     * <code>"0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7"</code>
     * which is the address of underlying dApp.
     */
    @NonNull
    @JsonIgnore
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
     * Synonym to {@link #getKey()}.
     */
    @NonNull
    @JsonIgnore
    public String getCurrencyKey() {
        return getKey();
    }

    /**
     * Get the currency type of this currency: is this a top-level blockchain “ETH” coin,
     * or some token, like ERC20 token.
     */
    @JsonGetter("type")
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
     * Whether this currency is “verified”, i.e. has been confirmed by the system administrator to be properly set up.
     */
    public boolean getVerified() { return verified; }

    /**
     * Get the Ethereum address for the underlying dApp of this currency.
     * Returns <code>null</code> for Ethereum cryptocurrency.
     *
     * @implNote the address is lowercased, not in EIP55. Example return string:
     * <code>"0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7"</code>.
     */
    @JsonGetter("dAppAddress")
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

    /**
     * Get a gas limit for transfer operation.
     * Optional, may return <code>null</code> (but only if the currency is not verified).
     */
    @Nullable
    public BigInteger getTransferGasLimit() {
        return transferGasLimit;
    }
}
