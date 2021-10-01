package com.myodov.unicherrygarden.cherrygardener.connector.impl;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import com.myodov.unicherrygarden.cherrygardener.connector.api.Observer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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
    private final ActorSystem<ConnectorActor.Message> actorSystem;


    /**
     * Constructor.
     */
    public ObserverImpl(@NonNull ActorSystem<ConnectorActor.Message> actorSystem) {
        assert actorSystem != null;
        this.actorSystem = actorSystem;
    }

    @Override
    public boolean startTrackingAddress(@NonNull String address,
                                        @NonNull StartTrackingAddressMode mode,
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
//

        final CompletionStage<ConnectorActor.ListTrackedAddressesCommand.Result> stage =
                AskPattern.ask(
                        actorSystem,
                        ConnectorActor.ListTrackedAddressesCommand::new,
                        ConnectorActor.DEFAULT_CALL_TIMEOUT,
                        actorSystem.scheduler());

        try {
            final ConnectorActor.ListTrackedAddressesCommand.Result result = stage.toCompletableFuture().join();
            return result.response.addresses;
        } catch (CancellationException | CompletionException exc) {
            logger.error("Could not complete ListTrackedAddressesCommand command", exc);
            return null;
        }
    }

    @Override
    @NonNull
    public BalanceRequestResult getAddressBalance(@NonNull String address,
                                                  @Nullable Set<String> filterCurrencyKeys) {
        throw new RuntimeException("TODO: not implemented");
    }
}
