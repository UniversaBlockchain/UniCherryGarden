package com.myodov.unicherrygarden.connector.impl;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import com.myodov.unicherrygarden.api.Validators;
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
import com.myodov.unicherrygarden.messages.cherrypicker.GetAddressDetails;
import com.myodov.unicherrygarden.messages.cherryplanter.PlantTransaction;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import static com.myodov.unicherrygarden.NullTools.coalesce;

/**
 * The default implementation for {@link Sender} interface.
 */
public final class SenderImpl implements Sender {
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
    public UnsignedOutgoingTransfer createOutgoingTransfer(
            @Nullable String sender,
            @NonNull String receiver,
            @NonNull String currencyKey,
            @NonNull BigDecimal amount,
            @Nullable Integer forceDecimals,
            @Nullable Long forceChainId,
            @Nullable BigInteger forceGasLimit,
            @Nullable BigInteger forceNonce,
            @Nullable BigDecimal forceMaxPriorityFee,
            @Nullable BigDecimal forceMaxFee
    ) {
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

        // Detect gas limit / decimals at once
        final int decimals;
        final BigInteger gasLimit;
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
            } else {
                assert !offlineMode;

                logger.debug("Need to discover gas limit/decimals for currency \"{}\"", currencyKey);

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

                    // We've read the currency details; we know know the currency decimals and gas limit values
                    // which are defined on the server.
                    // If “force” values were requested, we use them; otherwise we use the received values.

                    decimals = (guessDecimals != null) ? guessDecimals : currencyDetails.getDecimals();
                    gasLimit = (forceGasLimit != null) ? forceGasLimit : currencyDetails.getTransferGasLimit();
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

        logger.debug("Will use Chain ID {}, gas limit {}, nonce {}, decimals {}",
                chainId, gasLimit, nonce, decimals);

        // TODO: calculation/estimation
        final BigDecimal maxPriorityFee =
                (forceMaxPriorityFee != null) ?
                        forceMaxPriorityFee :
                        EthUtils.Wei.valueFromGweis(BigDecimal.valueOf(2));
        final BigDecimal maxFee =
                (forceMaxFee != null) ?
                        forceMaxFee :
                        EthUtils.Wei.valueFromGweis(BigDecimal.valueOf(100));

        final UnsignedOutgoingTransfer result;
        if (currencyKey.isEmpty()) {
            // ETH or other base currency
            result = UnsignedOutgoingTransfer.createEtherTransfer(
                    receiver,
                    amount,
                    chainId,
                    nonce,
                    maxPriorityFee,
                    maxFee
            );
        } else {
            assert gasLimit != null : gasLimit;

            // Currently the only other option is ERC20
            result = UnsignedOutgoingTransfer.createERC20Transfer(
                    receiver,
                    amount,
                    decimals,
                    currencyKey,
                    chainId,
                    nonce,
                    gasLimit,
                    maxPriorityFee,
                    maxFee
            );
        }

        return result;
    }

    @Override
    @NonNull
    public SignedOutgoingTransfer signTransaction(
            @NonNull UnsignedOutgoingTransfer tx,
            byte[] privateKey) {
        return tx.sign(new PrivateKeyImpl(privateKey));
    }

    @Override
    public PlantTransaction.@NonNull Response sendTransaction(
            @NonNull SignedOutgoingTransfer tx,
            @Nullable String comment
    ) {
        if (offlineMode) {
            throw new UniCherryGardenError.NotAvailableInOfflineModeError("Cannot execute sendTransaction!");
        }

        final CompletionStage<PlantTransactionCommand.Result> stage =
                AskPattern.ask(
                        actorSystem,
                        PlantTransactionCommand.createReplier(tx, comment),
                        ConnectorActor.DEFAULT_CALL_TIMEOUT,
                        actorSystem.scheduler());

        try {
            return stage.toCompletableFuture().join().response;
        } catch (CancellationException | CompletionException exc) {
            logger.error("Could not complete GetAddressDetailsCommand command", exc);
            return PlantTransaction.Response.fromCommonFailure(FailurePayload.CANCELLATION_COMPLETION_FAILURE);
        }
    }
}
