package com.myodov.unicherrygarden.api.types.planted.transactions;

import com.myodov.unicherrygarden.ethereum.EthUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.web3j.crypto.Hash;
import org.web3j.crypto.SignedRawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;

public class SignedOutgoingTransfer extends OutgoingTransfer {
    @NonNull
    public final String sender;

    /**
     * Primary constructor (from byte array).
     */
    @SuppressWarnings("unused")
    public SignedOutgoingTransfer(byte[] bytes,
                                  // Signed-specific component
                                  @NonNull String sender,
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
        super(true,
                bytes,
                // Components
                receiver,
                amount,
                currencyKey,
                chainId,
                nonce,
                gasLimit,
                maxPriorityFee,
                maxFee);
        assert sender != null && EthUtils.Addresses.isValidLowercasedAddress(sender) : sender;

        this.sender = sender.toLowerCase();
    }

    /**
     * Secondary constructor (from {@link SignedRawTransaction}).
     */
    @SuppressWarnings("unused")
    public SignedOutgoingTransfer(@NonNull SignedRawTransaction signedRawTransaction,
                                  // Signed-specific component
                                  @NonNull String sender,
                                  // Components
                                  @NonNull String receiver,
                                  @NonNull BigDecimal amount,
                                  @NonNull String currencyKey,
                                  long chainId,
                                  @NonNull BigInteger nonce,
                                  @NonNull BigInteger gasLimit,
                                  @NonNull BigDecimal maxPriorityFee,
                                  @NonNull BigDecimal maxFee) {
        this(TransactionEncoder.encode(signedRawTransaction),
                sender,
                receiver,
                amount,
                currencyKey,
                chainId,
                nonce,
                gasLimit,
                maxPriorityFee,
                maxFee);
    }

    @Override
    public String toString() {
        return String.format("%s(bytes=\"%s\": sender=%s, receiver=%s, amount=%s, currencyKey=%s, %s, %s, %s, %s, %s)",
                getClass().getSimpleName(),
                getBytesHexString(),
                sender, receiver, amount, currencyKey,
                chainId, nonce, gasLimit, maxPriorityFee, maxFee);
    }


    /**
     * Get tx hash (a string like <code>"0x8fc8b7de7cac3b2ae24ae2d67f35750bccf3d49996313f4d567929e6f6bef44c"</code>)
     * of the Ethereum transaction.
     */
    @NonNull
    public final String getHash() {
        return Numeric.toHexString(Hash.sha3(bytes));
    }
}
