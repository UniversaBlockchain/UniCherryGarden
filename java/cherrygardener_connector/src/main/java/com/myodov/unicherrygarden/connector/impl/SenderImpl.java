package com.myodov.unicherrygarden.connector.impl;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import com.myodov.unicherrygarden.api.Validators;
import com.myodov.unicherrygarden.api.types.SystemStatus;
import com.myodov.unicherrygarden.api.types.UniCherryGardenError;
import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.api.types.planted.transactions.SignedOutgoingTransfer;
import com.myodov.unicherrygarden.api.types.planted.transactions.UnsignedOutgoingTransfer;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload;
import com.myodov.unicherrygarden.connector.api.ClientConnector;
import com.myodov.unicherrygarden.connector.api.Sender;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActor;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActorMessage;
import com.myodov.unicherrygarden.connector.impl.actors.messages.PlantTransactionCommand;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import com.myodov.unicherrygarden.impl.types.PrivateKeyImpl;
import com.myodov.unicherrygarden.messages.cherrygardener.GetCurrencies;
import com.myodov.unicherrygarden.messages.cherrygardener.Ping;
import com.myodov.unicherrygarden.messages.cherrypicker.GetAddressDetails;
import com.myodov.unicherrygarden.messages.cherryplanter.PlantTransaction;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import static com.myodov.unicherrygarden.NullTools.coalesce;

/**
 * The default implementation for {@link Sender} interface.
 */
public final class SenderImpl implements Sender {

    public static final class FeeSuggestionImpl implements FeeSuggestion {

        @NonNull
        private final BigDecimal maxPriorityFee;

        @NonNull
        private final BigDecimal maxFee;

        FeeSuggestionImpl(@NonNull BigDecimal maxPriorityFee, @NonNull BigDecimal maxFee) {
            assert maxPriorityFee != null && maxPriorityFee.compareTo(BigDecimal.ZERO) >= 0 : maxPriorityFee;
            assert maxFee != null && maxFee.compareTo(BigDecimal.ZERO) >= 0 : maxFee;

            this.maxPriorityFee = maxPriorityFee;
            this.maxFee = maxFee;
        }

        @Override
        public String toString() {
            return String.format("FeeSuggestionImpl(%s, %s)", maxPriorityFee.toPlainString(), maxFee.toPlainString());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final SenderImpl.FeeSuggestionImpl that = (SenderImpl.FeeSuggestionImpl) o;
            return getMaxPriorityFee().equals(that.getMaxPriorityFee()) && getMaxFee().equals(that.getMaxFee());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getMaxPriorityFee(), getMaxFee());
        }

        @Override
        @NonNull
        public BigDecimal getMaxPriorityFee() {
            return maxPriorityFee;
        }

        @Override
        @NonNull
        public BigDecimal getMaxFee() {
            return maxFee;
        }

        /**
         * How much better than the rest we are going to be, setting the priority multiplier?
         * Should be at least 1.0; everything better is the improvement.
         */
        @NonNull
        private static final BigDecimal MAX_FEE_PER_GAS_PRIORITY_MULTIPLIER = new BigDecimal("1.5");

        @NonNull
        public static FeeSuggestionImpl fromBlockchainSystemStatus(SystemStatus.@NonNull Blockchain blockchainSystemStatus) {
            assert blockchainSystemStatus != null : blockchainSystemStatus;

            // maxPriorityFeePerGas * MAX_FEE_PER_GAS_PRIORITY_MULTIPLIER
            final BigDecimal maxPriorityFee = EthUtils.Wei.valueFromWeis(blockchainSystemStatus.maxPriorityFeePerGas).multiply(MAX_FEE_PER_GAS_PRIORITY_MULTIPLIER);
            // Use the following empirical way: max fee = 2 * nextBaseFee + priorityFee
            final BigDecimal maxFee = BigDecimal.valueOf(2).multiply(EthUtils.Wei.valueFromWeis(blockchainSystemStatus.latestBlock.nextBaseFeePerGas)).add(maxPriorityFee);

            return new FeeSuggestionImpl(maxPriorityFee, maxFee);
        }
    }

    final Logger logger = LoggerFactory.getLogger(SenderImpl.class);

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
     * @param clientConnector The instance of Client Connector used for some operations;
     *                        <code>null</code> (only) if created in offline mode.
     * @see <a href="https://github.com/ethereum/EIPs/blob/master/EIPS/eip-155.md">EIP-155</a>.
     */
    @SuppressWarnings("unused")
    public SenderImpl(@Nullable ClientConnectorImpl clientConnector) {
        this.clientConnector = clientConnector;
        this.actorSystem = (clientConnector == null) ? null : clientConnector.getActorSystem();
        this.offlineMode = clientConnector == null;

        logger.debug("Starting sender; will use client connector {}", clientConnector);
    }

    /**
     * Simple constructor, creating the Sender in “offline mode”.
     */
    public SenderImpl() {
        this(null);
    }


    @Override
    @NonNull
    public FeeSuggestion suggestFees() {
        return new FeeSuggestionImpl(EthUtils.Wei.valueFromGweis(BigDecimal.valueOf(2)), EthUtils.Wei.valueFromGweis(BigDecimal.valueOf(100)));
    }

    @Override
    @NonNull
    public UnsignedOutgoingTransfer createOutgoingTransfer(@Nullable String sender, @NonNull String receiver, @NonNull String currencyKey, @NonNull BigDecimal amount, @Nullable Integer forceDecimals, @Nullable Long forceChainId, @Nullable BigInteger forceGasLimit, @Nullable BigInteger forceNonce, @Nullable BigDecimal forceMaxPriorityFee, @Nullable BigDecimal forceMaxFee) {
        assert currencyKey != null : currencyKey;
        assert amount != null : amount;
        if (currencyKey.isEmpty()) {
            // for ETH, decimals may be either omitted or 18
            assert forceDecimals == null || forceDecimals == 18 : forceDecimals;
        } else {
            // for ERC20, forceDecimals may be either omitted/autodetected, or positive
            assert forceDecimals == null || forceDecimals > 0 : forceDecimals;
        }
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
            if (forceGasLimit == null) {
                throw new UniCherryGardenError.NotAvailableInOfflineModeError("forceGasLimit cannot be null!");
            }
            if (forceNonce == null) {
                throw new UniCherryGardenError.NotAvailableInOfflineModeError("forceNonce cannot be null!");
            }
            if (forceMaxPriorityFee == null) {
                throw new UniCherryGardenError.NotAvailableInOfflineModeError("forceMaxPriorityFee cannot be null!");
            }
            if (forceMaxFee == null) {
                throw new UniCherryGardenError.NotAvailableInOfflineModeError("forceMaxFee cannot be null!");
            }
        } else {
            assert clientConnector != null;
        }

        // Co-validations
        if (sender == null && forceNonce == null) {
            throw new UniCherryGardenError.ArgumentError("At least one of sender and forceNonce must be provided!");
        }

        // `if amount < 0`
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new UniCherryGardenError.ArgumentError(String.format("%s is not a valid amount", amount));
        }

        logger.debug("Going to create outgoing transfer of {} \"{}\" from {} to {}", amount, currencyKey, sender, receiver);

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

        // Detect gas limit / decimals at once
        final int decimals;
        final BigInteger gasLimit;
        // Also it is possible that we receive SystemStatus. It can be useful on later stages, we'll keep it.
        final Optional<SystemStatus> collateralSystemStatus;
        {
            // Can we guess decimals?
            @Nullable Integer guessDecimals;
            if (currencyKey.isEmpty()) {
                // For ETH, decimals is 18, definitely
                guessDecimals = 18;
            } else {
                guessDecimals = forceDecimals;
            }

            if (guessDecimals != null && forceGasLimit != null) {
                decimals = guessDecimals;
                gasLimit = forceGasLimit;
                collateralSystemStatus = Optional.empty();
            } else {
                assert !offlineMode;

                logger.debug("Need to discover gas limit/decimals for currency \"{}\"", currencyKey);

                final GetCurrencies.Response currencyResp = clientConnector.getCurrency(currencyKey);
                if (currencyResp.isFailure()) {
                    logger.error("When getting the details about currency {}, had a problem: {}", currencyKey, currencyResp.getFailure());
                    throw new UniCherryGardenError.NetworkError(String.format("A network problem arisen when getting the details about currency \"%s\": %s", currencyKey, currencyResp.getFailure()));
                } else {
                    final GetCurrencies.CurrenciesRequestResultPayload currencyPayload = currencyResp.getPayloadAsSuccessful();
                    assert currencyPayload.currencies.size() == 1 : currencyPayload.currencies;
                    final Currency currencyDetails = currencyPayload.currencies.iterator().next();
                    assert currencyDetails.getCurrencyKey().equals(currencyKey) : String.format("%s/%s", currencyKey, currencyDetails);

                    // We've read the currency details; we now know the currency decimals and gas limit values
                    // which are defined on the server.
                    // If “force” values were requested, we use them; otherwise we use the received values.

                    decimals = (guessDecimals != null) ? guessDecimals : currencyDetails.getDecimals();
                    gasLimit = (forceGasLimit != null) ? forceGasLimit : currencyDetails.getTransferGasLimit();
                    assert currencyPayload.systemStatus != null : currencyPayload;
                    collateralSystemStatus = Optional.of(currencyPayload.systemStatus);
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
                    logger.error("When getting the details about address {}, had a problem: {}", sender, addrDetailsResp.getFailure());
                    throw new UniCherryGardenError.NetworkError(String.format("A network problem arisen when getting the details about address %s: %s", sender, addrDetailsResp.getFailure()));
                } else {
                    final GetAddressDetails.AddressDetailsRequestResultPayload.AddressDetails addrDetails = addrDetailsResp.getPayloadAsSuccessful().details;

                    nonce = BigInteger.valueOf(coalesce(addrDetails.nonces.nextPlanting, addrDetails.nonces.nextInPendingPool, addrDetails.nonces.nextInBlockchain));
                }
            }
        }

        logger.debug("Will use Chain ID {}, gas limit {}, nonce {}, decimals {}", chainId, gasLimit, nonce, decimals);

        final BigDecimal maxPriorityFee;
        final BigDecimal maxFee;

        // If we don’t know/haven’t forced at least one of maxPriorityFee or maxFee, we need to discover them
        if (forceMaxPriorityFee == null || forceMaxFee == null) {
            assert !offlineMode;

            // Trying to discover the maxPriorityFee/maxFee.
            // Do we have a SystemStatus already?
            final SystemStatus systemStatus = collateralSystemStatus.orElseGet(() -> {
                final Ping.Response pingResp = clientConnector.ping();

                if (pingResp.isFailure()) {
                    logger.error("When doing the ping to get the system status, had a problem: {}", pingResp.getFailure());
                    throw new UniCherryGardenError.NetworkError(String.format("A network problem arisen when doing the ping: %s", pingResp.getFailure()));
                } else {
                    final Ping.PingRequestResultPayload payloadAsSuccessful = pingResp.getPayloadAsSuccessful();
                    return payloadAsSuccessful.systemStatus;
                }
            });

            final FeeSuggestion feeSuggestion = FeeSuggestionImpl.fromBlockchainSystemStatus(systemStatus.blockchain);
            logger.debug("Received fee suggestion: {}", feeSuggestion);

            maxPriorityFee = (forceMaxPriorityFee != null) ? forceMaxPriorityFee : feeSuggestion.getMaxPriorityFee();
            maxFee = (forceMaxFee != null) ? forceMaxFee : feeSuggestion.getMaxFee();
        } else {
            maxPriorityFee = forceMaxPriorityFee;
            maxFee = forceMaxFee;
        }
        logger.debug("Will be using max priority fee {}, max fee ", maxPriorityFee, maxFee);

        final UnsignedOutgoingTransfer result;
        if (currencyKey.isEmpty()) {
            // ETH or other base currency
            result = UnsignedOutgoingTransfer.createEtherTransfer(receiver, amount, chainId, nonce, maxPriorityFee, maxFee);
        } else {
            assert gasLimit != null : gasLimit;

            // Currently the only other option is ERC20
            result = UnsignedOutgoingTransfer.createERC20Transfer(receiver, amount, decimals, currencyKey, chainId, nonce, gasLimit, maxPriorityFee, maxFee);
        }

        return result;
    }

    @Override
    @NonNull
    public SignedOutgoingTransfer signTransaction(@NonNull UnsignedOutgoingTransfer tx, byte[] privateKey) {
        return tx.sign(new PrivateKeyImpl(privateKey));
    }

    @Override
    public PlantTransaction.@NonNull Response sendTransaction(@NonNull SignedOutgoingTransfer tx, @Nullable String comment) {
        if (offlineMode) {
            throw new UniCherryGardenError.NotAvailableInOfflineModeError("Cannot execute sendTransaction!");
        }

        final CompletionStage<PlantTransactionCommand.Result> stage = AskPattern.ask(actorSystem, PlantTransactionCommand.createReplier(tx, comment), ConnectorActor.DEFAULT_CALL_TIMEOUT, actorSystem.scheduler());

        try {
            return stage.toCompletableFuture().join().response;
        } catch (CancellationException | CompletionException exc) {
            logger.error("Could not complete GetAddressDetailsCommand command", exc);
            return PlantTransaction.Response.fromCommonFailure(FailurePayload.CANCELLATION_COMPLETION_FAILURE);
        }
    }
}
