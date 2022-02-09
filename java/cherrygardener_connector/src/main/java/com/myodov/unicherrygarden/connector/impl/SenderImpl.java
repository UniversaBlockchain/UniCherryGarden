package com.myodov.unicherrygarden.connector.impl;

import com.myodov.unicherrygarden.api.types.PrivateKey;
import com.myodov.unicherrygarden.api.types.UniCherryGardenError;
import com.myodov.unicherrygarden.connector.api.Sender;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import com.myodov.unicherrygarden.impl.types.PrivateKeyImpl;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.bouncycastle.util.encoders.Hex;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.SignedRawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * The default implementation for {@link Sender} interface.
 */
public class SenderImpl implements Sender {
    final Logger logger = LoggerFactory.getLogger(SenderImpl.class);

    static class ByteBasedOutgoingTransactionImpl implements PreparedOutgoingTransaction {

        protected final byte[] bytes;

        private final boolean isSigned;

        /**
         * Primary constructor (from byte array).
         */
        ByteBasedOutgoingTransactionImpl(boolean isSigned,
                                         byte[] bytes) {
            this.isSigned = isSigned;
            this.bytes = bytes.clone();
        }

        @Override
        public final boolean isSigned() {
            return isSigned;
        }

        @Override
        public final byte[] getBytes() {
            return bytes.clone();
        }

        @Override
        @NonNull
        public final String getBytesHexString() {
            // Slightly optimized, final and no-copy version
            return Hex.toHexString(bytes);
        }

        @Override
        @NonNull
        public final String getPublicRepresentation() {
            // Slightly optimized, final and no-copy version
            return Numeric.toHexString(bytes);
        }
    }

    /**
     * The default implementation for {@link UnsignedOutgoingTransaction} interface.
     */
    static class UnsignedOutgoingTransactionImpl
            extends ByteBasedOutgoingTransactionImpl
            implements UnsignedOutgoingTransaction {

        /**
         * Primary constructor (from byte array).
         */
        @SuppressWarnings("unused")
        UnsignedOutgoingTransactionImpl(byte[] bytes) {
            super(false,
                    bytes);
        }

        /**
         * Secondary constructor (from {@link RawTransaction}).
         */
        @SuppressWarnings("unused")
        UnsignedOutgoingTransactionImpl(@NonNull RawTransaction rawTransaction) {
            this(TransactionEncoder.encode(rawTransaction));
        }

        /**
         * Create a transaction to transfer the base currency of the blockchain
         * (in case of Ethereum Mainnet, this is ETH; may be different for other blockchains,
         * but for simplicity of referring to it let's call it “Ether Transaction”).
         *
         * @param receiver       the receiver of the transaction; i.e. the “to” field.
         *                       Should be a valid Ethereum address, upper or lower case,
         *                       e.g. <code>"0x34e1E4F805fCdC936068A760b2C17BC62135b5AE"</code>.
         * @param amount         amount of currency (ETH in case of Ethereum Mainnet) to be transferred.
         *                       This is the “end-user-interpretation” of the amount, i.e. the real number of ETH
         *                       with decimal point, rather than internal uint256-based number of weis.
         * @param nonce          nonce for the transaction.
         * @param maxPriorityFee (EIP-1559) Max Priority Fee; measured in ETH, but in real scenarios
         *                       it is often measured in Gweis; see the counterpart method.
         * @param maxFee         (EIP-1559) Max Fee; measured in ETH, but in real scenarios
         *                       it is often measured in Gweis; see the counterpart method.
         * @apiNote Read <a href="https://github.com/ethereum/EIPs/blob/master/EIPS/eip-1559.md">EIP-1559</a>
         * to get more details about Max Priority Fee and Max Fee.
         * Also, see the <a href="https://notes.ethereum.org/@vbuterin/eip-1559-faq">EIP-1559 FAQ</a>
         * for practical explanation.
         */
        @SuppressWarnings("unused")
        static UnsignedOutgoingTransactionImpl createEtherTransaction(
                @NonNull String receiver,
                @NonNull BigDecimal amount,
                long chainId,
                @NonNull BigInteger nonce,
                @NonNull BigDecimal maxPriorityFee,
                @NonNull BigDecimal maxFee
        ) {
            Validators.requireValidEthereumAddress("receiver", receiver);
            assert amount != null && amount.compareTo(BigDecimal.ZERO) >= 0 : amount; // amount >= 0
            Validators.requireValidNonce("nonce", nonce);
            assert maxPriorityFee != null && maxPriorityFee.compareTo(BigDecimal.ZERO) >= 0 : maxPriorityFee; // maxPriorityFee >= 0
            assert maxFee != null && maxFee.compareTo(BigDecimal.ZERO) >= 0 : maxFee; // maxFee >= 0

            return new UnsignedOutgoingTransactionImpl(RawTransaction.createEtherTransaction(
                    chainId,
                    nonce,
                    EthUtils.ETH_TRANSFER_GAS_LIMIT_BIGINTEGER,
                    receiver.toLowerCase(),
                    EthUtils.Wei.valueToWeis(amount),
                    EthUtils.Wei.valueToWeis(maxPriorityFee),
                    EthUtils.Wei.valueToWeis(maxFee)
            ));
        }

        /**
         * Create a transaction to transfer the base currency of the blockchain
         * (in case of Ethereum Mainnet, this is ETH; may be different for other blockchains,
         * but for simplicity of referring to it let's call it “Ether Transaction”).
         *
         * @param receiver           the receiver of the transaction; i.e. the “to” field.
         *                           Should be a valid Ethereum address, upper or lower case,
         *                           e.g. <code>"0x34e1E4F805fCdC936068A760b2C17BC62135b5AE"</code>.
         * @param amount             amount of currency (ETH in case of Ethereum Mainnet) to be transferred.
         *                           This is the “end-user-interpretation” of the amount, i.e. the real number of ETH
         *                           with decimal point, rather than internal uint256-based number of weis.
         * @param nonce              nonce for the transaction.
         * @param maxPriorityFeeGwei (EIP-1559) Max Priority Fee; measured in Gwei (conveniently for end user);
         *                           see the counterpart method if you want to measure it precisely in ETH
         *                           (or other base currency).
         * @param maxFeeGwei         (EIP-1559) Max Fee; measured in Gwei (conveniently for end user);
         *                           see the counterpart method if you want to measure it precisely in ETH
         *                           (or other base currency).
         * @apiNote Read <a href="https://github.com/ethereum/EIPs/blob/master/EIPS/eip-1559.md">EIP-1559</a>
         * to get more details about Max Priority Fee and Max Fee.
         * Also, see the <a href="https://notes.ethereum.org/@vbuterin/eip-1559-faq">EIP-1559 FAQ</a>
         * for practical explanation.
         */
        @SuppressWarnings("unused")
        static UnsignedOutgoingTransactionImpl createEtherTransaction(
                @NonNull String receiver,
                @NonNull BigDecimal amount,
                long chainId,
                @NonNull BigInteger nonce,
                long maxPriorityFeeGwei,
                long maxFeeGwei
        ) {
            Validators.requireValidEthereumAddress("receiver", receiver);
            assert amount != null && amount.compareTo(BigDecimal.ZERO) >= 0 : amount; // amount >= 0
            Validators.requireValidNonce("nonce", nonce);
            assert maxPriorityFeeGwei >= 0 : maxPriorityFeeGwei;
            assert maxFeeGwei >= 0 : maxFeeGwei;

            return createEtherTransaction(
                    receiver,
                    amount,
                    chainId,
                    nonce,
                    EthUtils.Wei.valueFromGweis(BigDecimal.valueOf(maxPriorityFeeGwei)),
                    EthUtils.Wei.valueFromGweis(BigDecimal.valueOf(maxFeeGwei))
            );
        }

        @Override
        @NonNull
        public final SignedOutgoingTransaction sign(@NonNull PrivateKey privateKey) {
            final byte[] signed = TransactionEncoder.signMessage(
                    getRawTransaction(), privateKey.getCredentials());
            return new SignedOutgoingTransactionImpl(signed);
        }
    }

    /**
     * The default implementation for {@link SignedOutgoingTransaction} interface.
     */
    static class SignedOutgoingTransactionImpl
            extends ByteBasedOutgoingTransactionImpl
            implements SignedOutgoingTransaction {

        /**
         * Primary constructor (from byte array).
         */
        @SuppressWarnings("unused")
        SignedOutgoingTransactionImpl(byte[] bytes) {
            super(true,
                    bytes);
        }

        /**
         * Secondary constructor (from {@link SignedRawTransaction}).
         */
        @SuppressWarnings("unused")
        SignedOutgoingTransactionImpl(@NonNull SignedRawTransaction signedRawTransaction) {
            this(TransactionEncoder.encode(signedRawTransaction));
        }

        @Override
        @NonNull
        public final String getHash() {
            return Numeric.toHexString(Hash.sha3(bytes));
        }
    }

    private static final Config config = ConfigFactory.load();

    protected final long chainId;

    /**
     * Constructor.
     *
     * @param chainId Chain ID (EIP-155) which the sender will use to create/sign the transactions.
     *                Values from {@link org.web3j.tx.ChainIdLong} (or any other ones) can be used.
     *                If <code>null</code>, the default Chain ID configured in the library (Ethereum Mainnet)
     *                will be used.
     * @see <a href="https://github.com/ethereum/EIPs/blob/master/EIPS/eip-155.md">EIP-155</a>.
     */
    @SuppressWarnings("unused")
    public SenderImpl(@Nullable Long chainId) {
        if (chainId != null) {
            this.chainId = chainId;
        } else {
            this.chainId = config.getLong("unicherrygarden.ethereum.chain_id");
        }

        logger.debug("Starting sender; will use EIP-155 Chain ID {}", this.chainId);
    }

    /**
     * Constructor with defaults.
     * <p>
     * Defaults:
     * <ul>
     * <li>Chain ID – the default Chain ID configured in the library (Ethereum Mainnet).</li>
     * </ul>
     */
    @SuppressWarnings("unused")
    public SenderImpl() {
        this(null);
    }

    @Override
    @NonNull
    public final UnsignedOutgoingTransaction buildTransaction(
            @NonNull String receiver,
            @NonNull String currencyCode,
            @NonNull BigDecimal amount) {
        assert currencyCode != null : currencyCode;
        assert amount != null : amount;
        assert receiver != null : receiver;

        // TODO:
        // 1. Nonce calculation
        // 2. Gas limit (hardcoded for ETH; database-stored for ERC20).
        // 3. Gas price estimator

        Validators.requireValidCurrencyCode("currencyCode", currencyCode);
        // `if amount < 0`
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new UniCherryGardenError.ArgumentError(String.format("%s is not a valid amount", amount));
        }
        Validators.requireValidEthereumAddress("receiver", receiver);

        final UnsignedOutgoingTransaction result;
        if (currencyCode.isEmpty()) {
            // ETH or other base currency
            final BigInteger nonce = BigInteger.ZERO;
            final BigDecimal maxPriorityFee = new BigDecimal("1.2345E-14");
            final BigDecimal maxFee = new BigDecimal("6.789E-14");

            result = UnsignedOutgoingTransactionImpl.createEtherTransaction(
                    receiver,
                    amount,
                    chainId,
                    nonce,
                    maxPriorityFee,
                    maxFee
            );
        } else {
            // Currently the only other option is ERC20
            result = new UnsignedOutgoingTransactionImpl(new byte[0]);
        }

        return result;
    }

    @Override
    @NonNull
    public final SignedOutgoingTransaction signTransaction(
            @NonNull UnsignedOutgoingTransaction tx,
            byte[] privateKey) {
        return tx.sign(new PrivateKeyImpl(privateKey));
    }

    @Override
    @NonNull
    public SentOutgoingTransaction sendTransaction(@NonNull SignedOutgoingTransaction tx) {
        // TODO
        return null;
    }
}