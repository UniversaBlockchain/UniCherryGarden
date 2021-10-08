package com.myodov.unicherrygarden.messages.connector.impl;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import com.myodov.unicherrygarden.messages.cherrypicker.AddTrackedAddresses;
import com.myodov.unicherrygarden.messages.cherrypicker.GetBalances;
import com.myodov.unicherrygarden.messages.cherrypicker.GetTrackedAddresses;
import com.myodov.unicherrygarden.messages.connector.api.Observer;
import com.myodov.unicherrygarden.messages.connector.impl.actors.ConnectorActor;
import com.myodov.unicherrygarden.messages.connector.impl.actors.ConnectorActorMessage;
import com.myodov.unicherrygarden.messages.connector.impl.actors.messages.GetBalancesCommand;
import com.myodov.unicherrygarden.messages.connector.impl.actors.messages.GetTrackedAddressesCommand;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        throw new RuntimeException("TODO: not implemented");
    }

    @Override
    @Nullable
    public List<@NonNull String> getTrackedAddresses() {
        // TODO: implement real data
//        return new ArrayList<String>() {{
//            add("0x884191033518be08616821d7676ca01695698451");
//            add("0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24");
//            add("0x3452519f4711703e13ea0863487eb8401bd6ae57");
//        }};

        final CompletionStage<GetTrackedAddressesCommand.Result> stage =
                AskPattern.ask(
                        actorSystem,
                        (replyTo) -> new GetTrackedAddressesCommand(
                                replyTo,
                                new GetTrackedAddresses.GTARequestPayload(
                                        false,
                                        false,
                                        false
                                )),
                        ConnectorActor.DEFAULT_CALL_TIMEOUT,
                        actorSystem.scheduler());

        try {
            final GetTrackedAddressesCommand.Result result = stage.toCompletableFuture().join();
            final GetTrackedAddresses.Response response = result.response;
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
    @NonNull
    public BalanceRequestResult getAddressBalances(@NonNull String address,
                                                   @Nullable Set<String> filterCurrencyKeys,
                                                   int confirmations) {
        final CompletionStage<GetBalancesCommand.Result> stage =
                AskPattern.ask(
                        actorSystem,
                        (replyTo) -> new GetBalancesCommand(replyTo, new GetBalances.GBRequestPayload(confirmations)),
                        ConnectorActor.DEFAULT_CALL_TIMEOUT,
                        actorSystem.scheduler());

        throw new RuntimeException("TODO: not implemented");
    }
}
