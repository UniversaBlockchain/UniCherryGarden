package com.myodov.unicherrygarden.connector.impl;

import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.ClusterCommand;
import akka.cluster.typed.JoinSeedNodes;
import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.connector.api.AddressOwnershipConfirmator;
import com.myodov.unicherrygarden.connector.api.ClientConnector;
import com.myodov.unicherrygarden.connector.api.Keygen;
import com.myodov.unicherrygarden.connector.api.Observer;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActor;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActorMessage;
import com.myodov.unicherrygarden.connector.impl.actors.messages.GetCurrenciesCommand;
import com.myodov.unicherrygarden.connector.impl.actors.messages.WaitForBootCommand;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * The implementation of CherryGardener client connector API.
 *
 * @implSpec The default implementation is the primary API for CherryGardener calls;
 * and by default, it uses {@link ConnectorActor} as the primary
 */
public class ClientConnectorImpl implements ClientConnector {
    @NonNull
    protected static final AddressOwnershipConfirmator confirmator = new AddressOwnershipConfirmatorImpl();
    @NonNull
    protected static final Keygen keygen = new KeygenImpl();
    @Nullable
    protected final Observer observer;

    public static final Duration LAUNCH_TIMEOUT = Duration.ofSeconds(10);

    final Logger logger = LoggerFactory.getLogger(ClientConnectorImpl.class);


    @NonNull
    private final ActorSystem<ConnectorActorMessage> actorSystem;

    private final boolean offlineMode;

    /**
     * Primary and most detailed constructor of CherryGardener client connector.
     *
     * @param gardenerUrls           the list of URLs of CherryGardener service (strings containing host and port),
     *                               e.g. <code>List.of("127.0.0.1:2551", "127.0.0.1:2552")</code>.
     *                               If the list is empty, the connector is executed in “offline mode”,
     *                               capable to perform only the operations which are executed locally.
     * @param listenPort             IP the client should listen to. Between 0 (exclusive) and 65535 (inclusive).
     *                               Note your client (at port) and the CherryGardener service URLs should be mutually
     *                               reachable.
     * @param mandatoryConfirmations the necessary number of Ethereum block confirmations to be sure
     *                               in any blockchain data stability.
     *                               None of the functionality (getting balances, getting the list of transfers, etc)
     *                               will return any data confirmed with less than this amount of confirmations/blocks.
     *                               <p>
     *                               Some API calls may let you add extra number of confirmations on top of this number.
     *                               0 confirmations (should not be used!) means the transaction is considered valid
     *                               if it is available in the latest mined block; 1 confirmation for a transaction
     *                               means there is a 1 block mined <i>after</i> the transaction.
     *                               <p>
     *                               conds.
     *                               <p>
     *                               Some numbers to consider:
     *                               <ul>
     *                               <li>5 confirmations – often been considered as “everyday level of security”.</li>
     *                               <li>12 confirmations – often been considered as “everyday level of security”.</li>
     *                               <li>20 confirmations – used on large exchange Kraken for most transactions.</li>
     *                               <li>250, 375, even 500 confirmations – used by most conservative crypto
     *                               exchanges.</li>
     *                               </ul>
     * @throws CompletionException if failed to initialize.
     */
    public ClientConnectorImpl(@NonNull List<String> gardenerUrls,
                               int listenPort,
                               int mandatoryConfirmations) throws CompletionException {
        if (gardenerUrls == null) {
            throw new IllegalArgumentException("gardenerUrls should not be null! " +
                    "Pass an empty list of URLs if you want an offline mode");
        }
        if (!gardenerUrls.isEmpty()) {
            // The listenPort and mandatoryConfirmations validations are relevant only if non-offline mode
            if (!(65535 >= listenPort && listenPort > 0)) {
                throw new IllegalArgumentException("When in non-offline mode, listenPort should be " +
                        "between 0 exclusive and 65535 inclusive!");
            }
            if (mandatoryConfirmations < 0) {
                throw new IllegalArgumentException("When in non-offline mode, mandatoryConfirmations should be " +
                        "at least 0, the higher the better!");
            }
        }

        logger.info("Launching CherryGardener connector: gardener urls {}", gardenerUrls);
        offlineMode = gardenerUrls.isEmpty();

        actorSystem = ActorSystem.create(ConnectorActor.create(), "CherryGarden");
        if (offlineMode) {
            this.observer = null;
            logger.warn("Creating Connector in offline mode!");
        } else {
            this.observer = new ObserverImpl(actorSystem);

            final List<Address> seedNodes = gardenerUrls.stream()
                    .map(url -> AddressFromURIString.parse(String.format("akka://CherryGarden@%s", url)))
                    .collect(Collectors.toList());
            logger.info("Connecting to {}", seedNodes);

            final Cluster cluster = Cluster.get(actorSystem);
            final ActorRef<ClusterCommand> clusterManager = cluster.manager();
            clusterManager.tell(new JoinSeedNodes(seedNodes));

            logger.debug("Waiting to boot...");
            try {
                final CompletionStage<WaitForBootCommand.BootCompleted> stage = AskPattern.ask(
                        actorSystem,
                        WaitForBootCommand::new,
                        LAUNCH_TIMEOUT,
                        actorSystem.scheduler());
                stage.toCompletableFuture().join();
            } catch (CompletionException | CancellationException exc) {
                logger.error("Could not boot ClientConnector; shutting down Akka", exc);
                shutdown();
                throw exc;
            }
            logger.debug("Boot is completed!");
        }
    }

    /**
     * Simplified constructor, with safe defaults (24 confirmations, around 6 minutes).
     *
     * @param gardenerUrls the list of URLs of CherryGardener service (strings containing host and port),
     *                     e.g. <code>List.of("127.0.0.1:2551", "127.0.0.1:2552")</code>.
     *                     If the list is empty, the connector is executed in “offline mode”,
     *                     capable to perform only the operations which are executed locally.
     * @param listenPort   IP the client should listen to. Between 0 (exclusive) and 65535 (inclusive).
     *                     Note your client (at port) and the CherryGardener service URLs should be mutually
     *                     reachable.
     * @throws CompletionException if failed to initialize.
     */
    @SuppressWarnings("unused")
    public ClientConnectorImpl(@NonNull List<String> gardenerUrls,
                               int listenPort) throws CompletionException {
        this(gardenerUrls, listenPort, 24);
    }

    /**
     * Convenient constructor to create a connector in offline mode.
     *
     * @throws CompletionException if failed to initialize.
     */
    @SuppressWarnings("unused")
    public static ClientConnector createOfflineConnector() throws CompletionException {
        return new ClientConnectorImpl(
                Collections.emptyList(),
                0,
                0
        );
    }

    @Override
    public void shutdown() {
        actorSystem.terminate();
    }

    //
    // Methods that return the engines/subsystems
    //

    @Override
    @NonNull
    public AddressOwnershipConfirmator getAddressOwnershipConfirmator() {
        return confirmator;
    }

    @Override
    @NonNull
    public Keygen getKeygen() {
        return keygen;
    }

    @Override
    @Nullable
    public Observer getObserver() {
        // “Observer is null/not initialized” if and only if “offlineMode is enabled”.
        assert offlineMode == (observer == null) :
                String.format("%s, %s", offlineMode, observer);
        return observer;
    }

    //
    // Methods that return the actual business logic data.
    //

    @Override
    @Nullable
    public List<Currency> getCurrencies(boolean getVerified, boolean getUnverified) {
        if (offlineMode) {
            return null;
        } else {
            final CompletionStage<GetCurrenciesCommand.Result> stage =
                    AskPattern.ask(
                            actorSystem,
                            GetCurrenciesCommand.createReplier(getVerified, getUnverified),
                            ConnectorActor.DEFAULT_CALL_TIMEOUT,
                            actorSystem.scheduler());

            try {
                final GetCurrenciesCommand.Result result = stage.toCompletableFuture().join();
                return result.response.currencies;
            } catch (CancellationException | CompletionException exc) {
                logger.error("Could not complete GetCurrenciesCommand command", exc);
                return null;
            }
        }
    }
}
