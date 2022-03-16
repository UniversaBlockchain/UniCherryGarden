package com.myodov.unicherrygarden.api.types.planted.transactions;

import com.myodov.unicherrygarden.api.Validators;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * An outgoing transfer with its details; may be signed or unsigned.
 */
public abstract class OutgoingTransfer extends PreparedOutgoingTransaction {
    @NonNull
    public final String receiver;

    @NonNull
    public final BigDecimal amount;

    @NonNull
    public final String currencyKey;

    public final long chainId;

    @NonNull
    public final BigInteger nonce;

    @NonNull
    public final BigInteger gasLimit;

    @NonNull
    public final BigDecimal maxPriorityFee;

    @NonNull
    public final BigDecimal maxFee;

    /**
     * Primary constructor.
     */
    @SuppressWarnings("unused")
    public OutgoingTransfer(boolean signed,
                            byte[] bytes,
                            // Components
                            @NonNull String receiver,
                            @NonNull BigDecimal amount,
                            @NonNull String currencyKey,
                            long chainId,
                            @NonNull BigInteger nonce,
                            @NonNull BigInteger gasLimit,
                            @NonNull BigDecimal maxPriorityFee,
                            @NonNull BigDecimal maxFee
    ) {
        super(signed, bytes);
        assert receiver != null && EthUtils.Addresses.isValidLowercasedAddress(receiver): receiver;
        assert amount != null : amount;
        assert currencyKey != null && Validators.isValidCurrencyKey(currencyKey) : currencyKey;
        assert chainId == -1 || chainId >= 1 : chainId;
        assert nonce.compareTo(BigInteger.ZERO) >= 0 : nonce; // nonce >= BigInteger.ZERO
        assert gasLimit.compareTo(BigInteger.ZERO) >= 0 : gasLimit; // gasLimit >= BigInteger.ZERO
        assert maxPriorityFee.compareTo(BigDecimal.ZERO) >= 0 : maxPriorityFee; // maxPriorityFee >= BigDecimal.ZERO
        assert maxFee.compareTo(BigDecimal.ZERO) >= 0 : maxFee; // maxFee >= BigDecimal.ZERO

        this.receiver = receiver.toLowerCase();
        this.amount = amount;
        this.currencyKey = currencyKey.toLowerCase();
        this.chainId = chainId;
        this.nonce = nonce;
        this.gasLimit = gasLimit;
        this.maxPriorityFee = maxPriorityFee;
        this.maxFee = maxFee;
    }

    @Override
    public String toString() {
        return String.format("%s(bytes=\"%s\": receiver=%s, amount=%s, currencyKey=%s, %s, %s, %s, %s, %s)",
                getClass().getSimpleName(),
                getBytesHexString(),
                receiver, amount, currencyKey,
                chainId, nonce, gasLimit, maxPriorityFee, maxFee);
    }
}
