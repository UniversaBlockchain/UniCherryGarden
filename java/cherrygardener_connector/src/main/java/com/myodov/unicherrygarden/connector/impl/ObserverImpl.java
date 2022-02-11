package com.myodov.unicherrygarden.connector.impl;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import com.myodov.unicherrygarden.api.types.UniCherryGardenError;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload;
import com.myodov.unicherrygarden.connector.api.Observer;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActor;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActorMessage;
import com.myodov.unicherrygarden.connector.impl.actors.messages.AddTrackedAddressesCommand;
import com.myodov.unicherrygarden.connector.impl.actors.messages.GetBalancesCommand;
import com.myodov.unicherrygarden.connector.impl.actors.messages.GetTrackedAddressesCommand;
import com.myodov.unicherrygarden.connector.impl.actors.messages.GetTransfersCommand;
import com.myodov.unicherrygarden.messages.cherrypicker.AddTrackedAddresses;
import com.myodov.unicherrygarden.messages.cherrypicker.GetBalances;
import com.myodov.unicherrygarden.messages.cherrypicker.GetTrackedAddresses;
import com.myodov.unicherrygarden.messages.cherrypicker.GetTransfers;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * The default implementation for {@link Observer} interface.
 */
public class ObserverImpl implements Observer {
    final Logger logger = LoggerFactory.getLogger(ObserverImpl.class);

    @NonNull
    private final ActorSystem<ConnectorActorMessage> actorSystem;

    protected final int mandatoryConfirmations;


    /**
     * Constructor.
     */
    public ObserverImpl(@NonNull ActorSystem<ConnectorActorMessage> actorSystem,
                        int mandatoryConfirmations) {
        assert actorSystem != null;
        assert mandatoryConfirmations >= 0 : mandatoryConfirmations;
        this.actorSystem = actorSystem;
        this.mandatoryConfirmations = mandatoryConfirmations;
    }

    @Override
    public AddTrackedAddresses.@NonNull Response startTrackingAddress(
            @NonNull String address,
            AddTrackedAddresses.@NonNull StartTrackingAddressMode mode,
            @Nullable Integer blockNumber,
            @Nullable String comment) {
        assert address != null : address;
        Validators.requireValidLowercasedEthereumAddress("address", address);
        if ((mode == AddTrackedAddresses.StartTrackingAddressMode.FROM_BLOCK) != (blockNumber != null)) {
            throw new UniCherryGardenError.ArgumentError(String.format(
                    "Tracking mode (%s) should be FROM_BLOCK if and only if blockNumber (%s) is not null!",
                    mode, blockNumber));
        }

        final CompletionStage<AddTrackedAddressesCommand.Result> stage =
                AskPattern.ask(
                        actorSystem,
                        AddTrackedAddressesCommand.createReplier(
                                mode,
                                new ArrayList<AddTrackedAddresses.AddressDataToTrack>() {{
                                    add(new AddTrackedAddresses.AddressDataToTrack(address, comment));
                                }},
                                blockNumber
                        ),
                        ConnectorActor.DEFAULT_CALL_TIMEOUT,
                        actorSystem.scheduler());

        try {
            return stage.toCompletableFuture().join().response;
        } catch (CancellationException | CompletionException exc) {
            logger.error("Could not complete AddTrackedAddressesCommand command", exc);
            return AddTrackedAddresses.Response.fromCommonFailure(FailurePayload.CANCELLATION_COMPLETION_FAILURE);
        }
    }

    @Override
    public GetTrackedAddresses.@NonNull Response getTrackedAddresses() {
        final CompletionStage<GetTrackedAddressesCommand.Result> stage =
                AskPattern.ask(
                        actorSystem,
                        GetTrackedAddressesCommand.createReplier(
                                false,
                                false
                        ),
                        ConnectorActor.DEFAULT_CALL_TIMEOUT,
                        actorSystem.scheduler());

        try {
            return stage.toCompletableFuture().join().response;
        } catch (CancellationException | CompletionException exc) {
            logger.error("Could not complete GetTrackedAddressesCommand command", exc);
            return GetTrackedAddresses.Response.fromCommonFailure(FailurePayload.CANCELLATION_COMPLETION_FAILURE);
        }
    }

    @Override
    public GetBalances.@NonNull Response getAddressBalances(
            int confirmations,
            @NonNull String address,
            @Nullable Set<String> filterCurrencyKeys) {
        assert confirmations >= 0 && confirmations + mandatoryConfirmations >= 0:
                String.format("%s/%s", confirmations, mandatoryConfirmations);
        assert address != null : address;
        Validators.requireValidLowercasedEthereumAddress("address", address);
        Validators.requireValidBlockNumber("confirmations", confirmations);

        final CompletionStage<GetBalancesCommand.Result> stage =
                AskPattern.ask(
                        actorSystem,
                        GetBalancesCommand.createReplier(
                                confirmations + mandatoryConfirmations,
                                address,
                                filterCurrencyKeys),
                        ConnectorActor.DEFAULT_CALL_TIMEOUT,
                        actorSystem.scheduler());

        try {
            return stage.toCompletableFuture().join().response;
        } catch (CancellationException | CompletionException exc) {
            logger.error("Could not complete GetBalances command", exc);
            return GetBalances.Response.fromCommonFailure(FailurePayload.CANCELLATION_COMPLETION_FAILURE);
        }
    }

    @Override
    public GetTransfers.@NonNull Response getTransfers(
            int confirmations,
            @Nullable String sender,
            @Nullable String receiver,
            @Nullable Integer startBlock,
            @Nullable Integer endBlock,
            @Nullable Set<String> filterCurrencyKeys,
            boolean includeBalances
    ) {
        // Validations
        assert confirmations >= 0 && confirmations + mandatoryConfirmations >= 0:
                String.format("%s/%s", confirmations, mandatoryConfirmations);

        if (sender != null) Validators.requireValidLowercasedEthereumAddress("sender", sender);
        if (receiver != null) Validators.requireValidLowercasedEthereumAddress("receiver", receiver);
        if (sender == null && receiver == null) {
            throw new UniCherryGardenError.ArgumentError("At least sender or receiver must be specified!");
        }

        if (startBlock != null) Validators.requireValidBlockNumber("startBlock", startBlock);
        if (endBlock != null) Validators.requireValidBlockNumber("endBlock", endBlock);
        if (startBlock != null && endBlock != null && startBlock > endBlock) {
            throw new UniCherryGardenError.ArgumentError(String.format(
                    "If both are defined, startBlock (%d) must be <= endBlock (%d)!", startBlock, endBlock));
        }

        final CompletionStage<GetTransfersCommand.Result> stage =
                AskPattern.ask(
                        actorSystem,
                        GetTransfersCommand.createReplier(
                                confirmations + mandatoryConfirmations,
                                sender,
                                receiver,
                                startBlock,
                                endBlock,
                                filterCurrencyKeys,
                                includeBalances),
                        ConnectorActor.DEFAULT_CALL_TIMEOUT,
                        actorSystem.scheduler());
        try {
            return stage.toCompletableFuture().join().response;
        } catch (CancellationException | CompletionException exc) {
            logger.error("Could not complete GetTransfers command", exc);
            return GetTransfers.Response.fromCommonFailure(FailurePayload.CANCELLATION_COMPLETION_FAILURE);
        }
    }
}
