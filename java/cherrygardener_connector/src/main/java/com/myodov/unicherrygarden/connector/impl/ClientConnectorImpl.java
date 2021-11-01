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
import com.myodov.unicherrygarden.ethereum.Ethereum;
import org.bouncycastle.util.encoders.Hex;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.time.Duration;
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
     * Constructor of CherryGardener client connector.
     *
     * @param gardenerUrls the list of URLs of CherryGardener service (strings containing host and port),
     *                     e.g. <code>List.of("127.0.0.1:2551", "127.0.0.1:2552")</code>.
     *                     If the list is empty, the connector is executed in “offline mode”,
     *                     capable to perform only the operations which are executed locally.
     * @throws CompletionException if failed to initialize.
     */
    public ClientConnectorImpl(@NonNull List<String> gardenerUrls) throws CompletionException {
        assert gardenerUrls != null;
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

    @Override
    public void shutdown() {
        actorSystem.terminate();
    }

    /**
     * Main loop execution.
     */
    public static void main(String[] args) {
        // TODO: remove, not necessary
        final ECKeyPair pair;

        try {
            pair = Keys.createEcKeyPair();

            final String address = Keys.getAddress(pair);
            System.out.printf("Address: 0x%s\n", address);

            final byte[] bytes = Numeric.toBytesPadded(pair.getPrivateKey(), Ethereum.PRIVATE_KEY_SIZE_BYTES);
            System.out.printf("Private key: %s\n", Hex.toHexString(bytes));
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
        }
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
