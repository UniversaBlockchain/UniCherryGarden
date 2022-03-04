package com.myodov.unicherrygarden.connector.impl;

import akka.actor.typed.ActorSystem;
import com.myodov.unicherrygarden.api.types.PrivateKey;
import com.myodov.unicherrygarden.api.types.UniCherryGardenError;
import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.connector.api.ClientConnector;
import com.myodov.unicherrygarden.connector.api.Sender;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActorMessage;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import com.myodov.unicherrygarden.impl.types.PrivateKeyImpl;
import com.myodov.unicherrygarden.messages.cherrygardener.GetCurrencies;
import com.myodov.unicherrygarden.messages.cherrypicker.GetAddressDetails;
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

import static com.myodov.unicherrygarden.NullTools.coalesce;

/**
 * The default implementation for {@link Sender} interface.
 */
public final class SenderImpl implements Sender {
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
            Validators.requireValidEthereumAddress(receiver);
            assert amount != null && amount.compareTo(BigDecimal.ZERO) >= 0 : amount; // amount >= 0
            Validators.requireValidNonce(nonce);
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
            Validators.requireValidEthereumAddress(receiver);
            assert amount != null && amount.compareTo(BigDecimal.ZERO) >= 0 : amount; // amount >= 0
            Validators.requireValidNonce(nonce);
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

    /**
     * Null only if created in “offline mode”.
     */
    @Nullable
    private final ActorSystem<ConnectorActorMessage> actorSystem;

    /**
     * Null only if created in “offline mode”.
     */
    @Nullable
    private final ClientConnector clientConnector;

    /**
     * Whether the sender is created in “offline mode”.
     */
    private final boolean offlineMode;


    /**
     * Constructor.
     *
     * @param actorSystem     Akka actor system to use;
     *                        <code>null</code> (only) if created in offline mode.
     * @param clientConnector The instance of Client Connector used for some operations;
     *                        <code>null</code> (only) if created in offline mode.
     * @see <a href="https://github.com/ethereum/EIPs/blob/master/EIPS/eip-155.md">EIP-155</a>.
     */
    @SuppressWarnings("unused")
    public SenderImpl(@Nullable ActorSystem<ConnectorActorMessage> actorSystem,
                      @Nullable ClientConnector clientConnector) {
        assert (actorSystem == null) == (clientConnector == null) :
                String.format("%s/%s", actorSystem, clientConnector);

        this.actorSystem = actorSystem;
        this.clientConnector = clientConnector;

        this.offlineMode = (actorSystem == null) && (clientConnector == null);

        logger.debug("Starting sender; will use client connector {}", clientConnector);
    }

    /**
     * Simple constructor, creating the Sender in “offline mode”.
     */
    public SenderImpl() {
        this(null, null);
    }


    @Override
    @NonNull
    public UnsignedOutgoingTransaction createOutgoingTransfer(
            @Nullable String sender,
            @NonNull String receiver,
            @NonNull String currencyKey,
            @NonNull BigDecimal amount,
            @Nullable Long forceChainId,
            @Nullable BigInteger forceGasLimit,
            @Nullable BigInteger forceNonce
    ) {
        assert currencyKey != null : currencyKey;
        assert amount != null : amount;
        assert receiver != null : receiver;
        assert forceChainId == null || forceChainId == -1 || forceChainId >= 1 : forceChainId;
        assert forceGasLimit == null || forceGasLimit.compareTo(BigInteger.valueOf(21_000)) >= 0 : forceGasLimit;

        if (sender != null) {
            Validators.requireValidEthereumAddress(sender);
        }
        Validators.requireValidEthereumAddress(receiver);
        Validators.requireValidCurrencyKey(currencyKey);
        if (forceNonce != null) {
            Validators.requireValidNonce(forceNonce);
        }

        // Offline mode validations
        if (offlineMode) {
            if (forceChainId == null) {
                throw new UniCherryGardenError.NotAvailableInOfflineModeError("forceChainId cannot be null!");
            }
            if (forceNonce == null) {
                throw new UniCherryGardenError.NotAvailableInOfflineModeError("forceNonce cannot be null!");
            }
            if (forceGasLimit == null) {
                throw new UniCherryGardenError.NotAvailableInOfflineModeError("forceGasLimit cannot be null!");
            }
        }

        // Co-validations
        if (sender == null && forceNonce == null) {
            throw new UniCherryGardenError.ArgumentError("At least one of sender and forceNonce must be provided!");
        }

        // `if amount < 0`
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new UniCherryGardenError.ArgumentError(String.format("%s is not a valid amount", amount));
        }

        // TODO:
        // Gas price estimator

        logger.debug("Going to create outgoing transfer of {} \"{}\" from {} to {}",
                amount, currencyKey, sender, receiver);

        // Detect Chain ID (EIP-155) if it is not provided.
        final long chainId;
        {
            if (forceChainId != null) {
                chainId = forceChainId;
            } else {
                assert !offlineMode;
                chainId = clientConnector.getChainId();
            }
        }

        // Detect gas limit
        final BigInteger gasLimit;
        {
            if (forceGasLimit != null) {
                gasLimit = forceGasLimit;
            } else {
                assert !offlineMode;

                logger.debug("Need to discover gas limit for currency \"{}\"", currencyKey);

                final GetCurrencies.Response currencyResp = clientConnector.getCurrency(currencyKey);
                if (currencyResp.isFailure()) {
                    logger.error("When getting the details about currency {}, had a problem: {}",
                            currencyKey, currencyResp.getFailure());
                    throw new UniCherryGardenError.NetworkError(String.format(
                            "A network problem arisen when getting the details about currency \"%s\": %s",
                            currencyKey, currencyResp.getFailure()));
                } else {
                    final GetCurrencies.CurrenciesRequestResultPayload currencyPayload = currencyResp.getPayloadAsSuccessful();
                    assert currencyPayload.currencies.size() == 1 : currencyPayload.currencies;
                    final Currency currencyDetails = currencyPayload.currencies.iterator().next();
                    assert currencyDetails.getCurrencyKey().equals(currencyKey) : String.format("%s/%s", currencyKey, currencyDetails);

                    gasLimit = currencyDetails.getTransferGasLimit();
                }
            }
        }

        // Detect nonce
        final BigInteger nonce;
        {
            if (forceNonce != null) {
                nonce = forceNonce;
            } else {
                assert !offlineMode;
                assert sender != null;

                logger.debug("Need to discover nonce for address {}", sender);

                final GetAddressDetails.Response addrDetailsResp = clientConnector.getObserver().getAddressDetails(sender);
                if (addrDetailsResp.isFailure()) {
                    logger.error("When getting the details about address {}, had a problem: {}",
                            sender, addrDetailsResp.getFailure());
                    throw new UniCherryGardenError.NetworkError(String.format(
                            "A network problem arisen when getting the details about address %s: %s",
                            sender, addrDetailsResp.getFailure()));
                } else {
                    final GetAddressDetails.AddressDetailsRequestResultPayload.AddressDetails addrDetails =
                            addrDetailsResp.getPayloadAsSuccessful().details;

                    nonce = BigInteger.valueOf(coalesce(
                            addrDetails.nonces.nextPlanting,
                            addrDetails.nonces.nextInPendingPool,
                            addrDetails.nonces.nextInBlockchain
                    ));
                }
            }
        }

        logger.debug("Will use Chain ID {}, gas limit {}, nonce {}", chainId, gasLimit, nonce);

        final UnsignedOutgoingTransaction result;
        if (currencyKey.isEmpty()) {
            // ETH or other base currency
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
    public SignedOutgoingTransaction signTransaction(
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
