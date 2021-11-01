package com.myodov.unicherrygarden.connector.impl;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import com.myodov.unicherrygarden.connector.api.Observer;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActor;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActorMessage;
import com.myodov.unicherrygarden.connector.impl.actors.messages.AddTrackedAddressesCommand;
import com.myodov.unicherrygarden.connector.impl.actors.messages.GetBalancesCommand;
import com.myodov.unicherrygarden.connector.impl.actors.messages.GetTrackedAddressesCommand;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import com.myodov.unicherrygarden.messages.cherrypicker.AddTrackedAddresses;
import com.myodov.unicherrygarden.messages.cherrypicker.GetBalances;
import com.myodov.unicherrygarden.messages.cherrypicker.GetTrackedAddresses;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * The default implementation for {@link Observer} interface.
 */
public class ObserverImpl implements Observer {
    final Logger logger = LoggerFactory.getLogger(ObserverImpl.class);

    @NonNull
    private final ActorSystem<ConnectorActorMessage> actorSystem;


    /**
     * Constructor.
     */
    public ObserverImpl(@NonNull ActorSystem<ConnectorActorMessage> actorSystem) {
        assert actorSystem != null;
        this.actorSystem = actorSystem;
    }

    @Override
    public boolean startTrackingAddress(@NonNull String address,
                                        AddTrackedAddresses.@NonNull StartTrackingAddressMode mode,
                                        @Nullable Integer blockNumber,
                                        @Nullable String comment) {
        assert (address != null) && EthUtils.Addresses.isValidLowercasedAddress(address) : address;
        if (!EthUtils.Addresses.isValidLowercasedAddress(address)) {
            throw new RuntimeException(String.format(
                    "%s is not a properly formed lowercased Ethereum address!",
                    address));
        }

        if ((mode == AddTrackedAddresses.StartTrackingAddressMode.FROM_BLOCK) != (blockNumber != null)) {
            throw new RuntimeException(String.format(
                    "Tracking mode (%s) should be FROM_BLOCK if and only if blockNumber (%s) is not null!",
                    mode, blockNumber));
        }

        final CompletionStage<AddTrackedAddressesCommand.Result> stage =
                AskPattern.ask(
                        actorSystem,
                        (replyTo) -> AddTrackedAddressesCommand.create(
                                replyTo,
                                mode,
                                new ArrayList<AddTrackedAddresses.AddressDataToTrack>() {{
                                    add(new AddTrackedAddresses.AddressDataToTrack(address, comment));
                                }},
                                blockNumber
                        ),
                        ConnectorActor.DEFAULT_CALL_TIMEOUT,
                        actorSystem.scheduler());

        try {
            final AddTrackedAddresses.Response response = stage.toCompletableFuture().join().response;
            // We've requested just a single address. Therefore, the result `response.addresses`
            // should be a set containing just it.
            if (response.addresses.size() == 1 &&
                    response.addresses.iterator().next() == address) {
                return true;
            } else {
                logger.error("Received the weird response (not a single item {} but {}), " +
                                "treating as failure.",
                        address, response.addresses);
                return false;
            }
        } catch (CancellationException | CompletionException exc) {
            logger.error("Could not complete AddTrackedAddressesCommand command", exc);
            return false;
        }
    }

    @Override
    @Nullable
    public List<@NonNull String> getTrackedAddresses() {
        final CompletionStage<GetTrackedAddressesCommand.Result> stage =
                AskPattern.ask(
                        actorSystem,
                        (replyTo) -> GetTrackedAddressesCommand.create(
                                replyTo,
                                false,
                                false,
                                false
                        ),
                        ConnectorActor.DEFAULT_CALL_TIMEOUT,
                        actorSystem.scheduler());

        try {
            final GetTrackedAddresses.Response response = stage.toCompletableFuture().join().response;
            // We didn’t request the comments/syncedFrom/syncedTo,
            // so they should not be present in the response.
            assert response.includeComment == false : response;
            assert response.includeSyncedFrom == false : response;
            assert response.includeSyncedTo == false : response;
            return response.addresses.stream().map(trAddInf -> {
                // We didn’t request the comments/syncedFrom/syncedTo,
                // so they REALLY should not be present in the response.
                assert trAddInf.comment == null : trAddInf;
                assert trAddInf.syncedFrom == null : trAddInf;
                assert trAddInf.syncedTo == null : trAddInf;
                // But the address should be, and should be non-empty. And valid Ethereum address!
                assert (trAddInf.address != null) && EthUtils.Addresses.isValidLowercasedAddress(trAddInf.address) : trAddInf;
                return trAddInf.address;
            }).collect(Collectors.toList());
        } catch (CancellationException | CompletionException exc) {
            logger.error("Could not complete GetTrackedAddressesCommand command", exc);
            return null;
        }
    }

    @Override
    public GetBalances.@NonNull BalanceRequestResult getAddressBalances(@NonNull String address,
                                                                        @Nullable Set<String> filterCurrencyKeys,
                                                                        int confirmations) {
        final CompletionStage<GetBalancesCommand.Result> stage =
                AskPattern.ask(
                        actorSystem,
                        (replyTo) -> GetBalancesCommand.create(
                                replyTo,
                                confirmations,
                                filterCurrencyKeys),
                        ConnectorActor.DEFAULT_CALL_TIMEOUT,
                        actorSystem.scheduler());

        try {
            final GetBalances.Response response = stage.toCompletableFuture().join().response;
            return response.result;
        } catch (CancellationException | CompletionException exc) {
            logger.error("Could not complete GetAddressBalances command", exc);
            return GetBalances.BalanceRequestResult.unsuccessful();
        }
    }
}
